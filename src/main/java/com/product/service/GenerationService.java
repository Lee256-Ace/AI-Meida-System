package com.product.service;

import com.product.entity.GenerationTask;
import com.product.mapper.GenerationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final GenerationMapper generationMapper;
    private final RestTemplate restTemplate;
    private final ErrorLogService errorLogService;
    private final QwenWorkflowClient qwenWorkflowClient;

    @Value("${coze.workflow.base-url:http://localhost:5000}")
    private String cozeBaseUrl;

    @Value(" ${coze.workflow.endpoint-path:/run}")
    private String cozeEndpointPath;

    @Value("${coze.workflow.timeout-seconds:300}")
    private int cozeTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public interface StreamHandler {
        void onReasoning(String text);
        void onContent(String text);
        void onComplete();
        void onError(String error);
    }

    @Transactional
    public Long createTask(String prompt, String style, String theme) {
        GenerationTask task = new GenerationTask();
        task.setPrompt(prompt);
        task.setModel("qwen");
        task.setStatus(0);
        task.setStyle(style);
        task.setTheme(theme);
        task.setCreatedAt(LocalDateTime.now());
        generationMapper.insert(task);
        return task.getId();
    }

    private static final String[] PLATFORM_KEYS = {"douyin", "xiaohongshu", "video_account", "toutiao"};

    @Async
    public void processTask(Long taskId, String prompt, List<String> selectedPlatforms, String targetAudience, StreamHandler handler) {
        StringBuilder content = new StringBuilder();
        Set<String> platformFilter = (selectedPlatforms != null && !selectedPlatforms.isEmpty())
                ? new HashSet<>(selectedPlatforms) : null;

        try {
            if (handler != null) handler.onReasoning("正在生成多平台文案（抖音、小红书、视频号、今日头条）…");

            List<String> platformList = (selectedPlatforms != null && !selectedPlatforms.isEmpty())
                    ? selectedPlatforms
                    : Arrays.asList(PLATFORM_KEYS);
            List<String> labels = new ArrayList<>();
            for (String key : platformList) {
                String label = PLATFORM_LABELS.get(key);
                if (label != null) labels.add(label);
            }

            String systemPrompt = "你是资深自媒体创作专家，精通抖音、小红书、视频号、今日头条。\n"
                    + "为每个平台各写一篇 Markdown 文章，段首单独一行用【平台名】作标题（仅限：抖音、小红书、视频号、今日头条）。\n"
                    + "正文用 Markdown：#/##/### 标题层级、**加粗**关键词、- 列表、--- 分隔线，适当用 emoji。\n"
                    + "只输出文章本身，不要解释或前言。各平台字数与结构：\n"
                    + "■ 抖音 约250字：## 口语化钩子标题 + 短句分段(多emoji) + 互动引导 + --- + ### 3~5个#话题。\n"
                    + "■ 小红书 约380字：## 标题 + ### 引入/### 正文(种草、优缺点)/### 推荐建议(列表：推荐指数/性价比/适用人群)，多emoji口语化(姐妹们/宝子们)，结尾#话题。\n"
                    + "■ 视频号 约550字：## 设问标题 + ### 引言(共鸣)/### 正文(论述)/### 结语(升华)，面对面聊天语气、有金句。\n"
                    + "■ 今日头条 约600字：## 标题 + ### 导语(背景)/### 正文(数据事实)/### 结语(收束)，正式客观。";

            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("请根据以下主题，为每个平台分别撰写一篇 Markdown 格式的完整文章：\n").append(prompt).append("\n\n");
            userPrompt.append("目标读者：").append(targetAudience != null && !targetAudience.isBlank() ? targetAudience.trim() : "大众").append("\n");
            userPrompt.append("需要生成的平台：").append(String.join("、", labels)).append("\n\n");
            userPrompt.append("请开始输出。");

            qwenWorkflowClient.generateStream(systemPrompt, userPrompt.toString(), new QwenWorkflowClient.StreamHandler() {
                @Override
                public void onContent(String text) {
                    content.append(text);
                    if (handler != null) handler.onContent(text);
                }
                @Override
                public void onComplete(String fullText) {
                    String full = fullText != null ? fullText.trim() : "";
                    Map<String, String> platformCopies = extractPlatformSectionsFromContent(full, platformFilter);
                    String formatted;
                    if (platformCopies != null && !platformCopies.isEmpty()) {
                        formatted = formatFromPlatformCopies(platformCopies, platformFilter);
                    } else {
                        formatted = full;
                    }
                    saveResult(taskId, "", formatted, null, platformCopies);
                    if (handler != null) handler.onComplete();
                }
                @Override
                public void onError(String message) {
                    saveResult(taskId, "", content.toString(), message, null);
                    String display = errorLogService.logAndMessage("generate", "千问文案生成", message, null, "taskId=" + taskId);
                    if (handler != null) handler.onError(display);
                }
            });

        } catch (Exception e) {
            log.error("文案生成失败", e);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            saveResult(taskId, "", content.toString(), errMsg, null);
            String displayMsg = errorLogService.logAndMessage("generate", "千问文案生成", errMsg, e, "taskId=" + taskId);
            if (handler != null) handler.onError(displayMsg);
        }
    }

    /**
     * 调用 Coze 官方 stream_run 流式接口：POST /stream_run，Body 为 { topic, platform_select }。
     * 响应为流式：每行 data: {"id", "event", "data": { "type": "workflow_start|workflow_end|node_start|node_end|error|ping", ... }}。
     * workflow_end 时 data.output 为工作流整体输出，含 final_result。兼容 SSE（data: 前缀）与 NDJSON（纯 JSON 行）。
     */
    private Map<String, Object> postStreamRunAndParseSse(String url, HttpHeaders headers, Map<String, Object> payload) throws Exception {
        return restTemplate.execute(url, HttpMethod.POST,
            request -> {
                headers.forEach((k, v) -> request.getHeaders().addAll(k, v));
                byte[] body = objectMapper.writeValueAsBytes(payload);
                request.getBody().write(body);
            },
            response -> {
                if (response.getStatusCode() != HttpStatusCode.valueOf(200)) {
                    throw new RuntimeException("Coze stream_run 返回异常: " + response.getStatusCode());
                }
                Map<String, Object> accumulatedBody = null;
                Map<String, Object> lastAny = null;
                StringBuilder contentAccum = new StringBuilder();
                String toutiaoFromAnyLine = null;
                int dataLineCount = 0;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String json = null;
                        if (line.startsWith("data:")) {
                            json = line.substring(5).trim();
                        } else if (line.trim().startsWith("{")) {
                            json = line.trim();
                        }
                        if (json == null || json.isEmpty()) continue;
                        if ("[DONE]".equalsIgnoreCase(json) || "DONE".equalsIgnoreCase(json)) continue;
                        try {
                            Map<String, Object> obj = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                            lastAny = obj;
                            if (toutiaoFromAnyLine == null || toutiaoFromAnyLine.isBlank()) {
                                String fromObj = findToutiaoCopyRecursively(obj);
                                if (fromObj == null || fromObj.isBlank()) fromObj = findToutiaoMarkdownInTree(obj);
                                if (fromObj != null && !fromObj.isBlank()) toutiaoFromAnyLine = fromObj;
                            }
                            Map<String, Object> withFinal = findMapContainingFinalResult(obj);
                            if (withFinal == null) {
                                withFinal = extractWorkflowEndOutput(obj);
                            }
                            if (withFinal != null) {
                                enrichBodyWithMarkdownFromTree(obj, withFinal);
                                accumulatedBody = mergeBodyInto(accumulatedBody, withFinal);
                                if (log.isDebugEnabled()) {
                                    Object fr = withFinal.get("final_result");
                                    Object pm = withFinal.get("platform_markdowns");
                                    log.debug("Coze stream 收到 output 帧: final_result.keys={}, platform_markdowns.keys={}",
                                            fr instanceof Map ? ((Map<?, ?>) fr).keySet() : "n/a",
                                            pm instanceof Map ? ((Map<?, ?>) pm).keySet() : "n/a");
                                }
                            }
                            if (log.isDebugEnabled() && obj.get("data") instanceof Map) {
                                Object type = ((Map<?, ?>) obj.get("data")).get("type");
                                if ("workflow_end".equals(type) || "error".equals(type)) {
                                    log.debug("Coze stream 事件类型: {}", type);
                                }
                            }
                            Object c = extractContentFromData(obj);
                            if (c != null) contentAccum.append(c.toString());
                            dataLineCount++;
                            if (dataLineCount <= 2) log.debug("Coze stream data#{} keys: {}", dataLineCount, obj.keySet());
                        } catch (Exception e) {
                            if (dataLineCount < 3) log.debug("Coze stream data line (parse skip): {}...", json.length() > 120 ? json.substring(0, 120) + "..." : json);
                        }
                    }
                }
                if (accumulatedBody != null) {
                    injectToutiaoFromContentIfMissing(accumulatedBody, contentAccum.toString());
                    tryInjectToutiaoFromParsedObject(accumulatedBody, lastAny);
                    if (!hasToutiaoInBody(accumulatedBody) && toutiaoFromAnyLine != null && !toutiaoFromAnyLine.isBlank()) {
                        putToutiaoIntoBody(accumulatedBody, toutiaoFromAnyLine);
                    }
                    if (log.isDebugEnabled()) {
                        Object fr = accumulatedBody.get("final_result");
                        Object pm = accumulatedBody.get("platform_markdowns");
                        log.debug("Coze 流式解析完成: body.keys={}, final_result.keys={}, platform_markdowns.keys={}, hasToutiao={}",
                                accumulatedBody.keySet(),
                                fr instanceof Map ? ((Map<?, ?>) fr).keySet() : "n/a",
                                pm instanceof Map ? ((Map<?, ?>) pm).keySet() : "n/a",
                                hasToutiaoInBody(accumulatedBody));
                    }
                    if (!hasToutiaoInBody(accumulatedBody)) {
                        Object fr = accumulatedBody.get("final_result");
                        Object pm = accumulatedBody.get("platform_markdowns");
                        log.warn("今日头条仍缺失。body.keys={}, final_result.keys={}, platform_markdowns.keys={}, contentAccum.len={}, dataLineCount={}",
                                accumulatedBody.keySet(),
                                fr instanceof Map ? ((Map<?, ?>) fr).keySet() : "n/a",
                                pm instanceof Map ? ((Map<?, ?>) pm).keySet() : "n/a",
                                contentAccum.length(), dataLineCount);
                    }
                    return accumulatedBody;
                }
                if (contentAccum.length() > 0) {
                    Map<String, Object> fallback = new LinkedHashMap<>();
                    fallback.put("content", contentAccum.toString());
                    return fallback;
                }
                if (lastAny != null) {
                    Map<String, Object> fromOutput = extractBodyFromOutput(lastAny);
                    if (fromOutput != null) return fromOutput;
                    String anyText = extractAnyTextFromMap(lastAny);
                    if (anyText != null && !anyText.isBlank()) {
                        Map<String, Object> fallback = new LinkedHashMap<>();
                        fallback.put("content", anyText);
                        return fallback;
                    }
                }
                log.warn("Coze stream_run 未解析到 final_result 或 content，共 {} 条 data", dataLineCount);
                return lastAny;
            });
    }

    /** Coze 流式返回扁平帧 { type, run_id, output, time_cost_ms } 时，从 output 取出 body（含 final_result / markdown_content），避免返回整帧导致下游报错 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBodyFromOutput(Map<String, Object> frame) {
        if (frame == null) return null;
        Object outputVal = frame.get("output");
        if (outputVal instanceof Map) return (Map<String, Object>) outputVal;
        if (outputVal instanceof String) {
            Map<String, Object> parsed = parseJsonMap((String) outputVal);
            return parsed;
        }
        return null;
    }

    private static Object firstNonBlank(Map<String, Object> map, String key1, String key2) {
        Object v = map.get(key1);
        if (v != null && v.toString().trim().length() > 0) return v;
        v = map.get(key2);
        if (v != null && v.toString().trim().length() > 0) return v;
        return null;
    }

    /** 各平台在 body 中可能的 key（英文/中文/copy 后缀），用于递归查找 */
    private static final String[][] PLATFORM_SEARCH_KEYS = {
            {"douyin", "抖音", "douyin_copy", "douyinCopy"},
            {"xiaohongshu", "小红书", "xiaohongshu_copy", "xiaohongshuCopy"},
            {"video_account", "视频号", "video_account_copy", "videoAccountCopy"},
            {"toutiao", "今日头条", "toutiao_copy", "toutiaoCopy"}
    };

    /** 当 body 仅有 markdown_content / platform_markdowns 时，从 platform_markdowns 按平台 key 取出文案构建 platformCopies */
    @SuppressWarnings("unchecked")
    private Map<String, String> buildPlatformCopiesFromMarkdownsOnly(Map<String, Object> body, Set<String> platformFilter) {
        if (body == null) return null;
        Object pm = body.get("platform_markdowns");
        if (!(pm instanceof Map)) return null;
        Map<?, ?> pmMap = (Map<?, ?>) pm;
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : PLATFORM_KEYS) {
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            Object v = pmMap.get(key);
            if (v instanceof CharSequence) {
                String s = ((CharSequence) v).toString().trim();
                if (!s.isEmpty()) out.put(key, s);
            }
        }
        return out.isEmpty() ? null : out;
    }

    /** 在整棵 body 树中递归查找指定平台的文案（兼容 Coze 任意嵌套） */
    @SuppressWarnings("unchecked")
    private String findPlatformCopyRecursively(Map<String, Object> map, String[] keys) {
        if (map == null || keys == null) return null;
        for (String k : keys) {
            Object v = map.get(k);
            if (v instanceof CharSequence) {
                String s = v.toString().trim();
                if (!s.isEmpty()) return s;
            }
            if (v instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) v;
                Object copy = m.get("copy");
                if (copy != null && copy.toString().trim().length() > 0) return copy.toString().trim();
                Object content = m.get("content");
                if (content != null && content.toString().trim().length() > 0) return content.toString().trim();
            }
        }
        for (Object val : map.values()) {
            if (val instanceof Map) {
                String s = findPlatformCopyRecursively((Map<String, Object>) val, keys);
                if (s != null && !s.isEmpty()) return s;
            }
        }
        return null;
    }

    private String findToutiaoCopyRecursively(Map<String, Object> map) {
        return findPlatformCopyRecursively(map, PLATFORM_SEARCH_KEYS[3]);
    }

    /** 从 body.platform_markdowns 中取指定平台的 Markdown 文本（Coze 可能只返回此处而 final_result 缺该平台） */
    @SuppressWarnings("unchecked")
    private String getTextFromPlatformMarkdowns(Map<String, Object> body, String platformKey) {
        Object pm = body.get("platform_markdowns");
        if (!(pm instanceof Map)) return null;
        Object section = ((Map<?, ?>) pm).get(platformKey);
        return (section instanceof CharSequence && ((CharSequence) section).length() > 0) ? section.toString().trim() : null;
    }

    /** 在整棵 body 中递归查找 key 为 toutiao/今日头条 且值为非空字符串的 MD 文本（兜底任意嵌套结构） */
    @SuppressWarnings("unchecked")
    private String findToutiaoMarkdownInTree(Map<String, Object> map) {
        if (map == null) return null;
        for (String k : new String[]{"toutiao", "今日头条"}) {
            Object v = map.get(k);
            if (v instanceof CharSequence) {
                String s = v.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        for (Object val : map.values()) {
            if (val instanceof Map) {
                String s = findToutiaoMarkdownInTree((Map<String, Object>) val);
                if (s != null && !s.isEmpty()) return s;
            }
        }
        return null;
    }

    /** 从 body.markdown_content 中截取今日头条段落（## 今日头条 / ## 📰 今日头条 至下一 ## 或结尾） */
    private String extractToutiaoSectionFromMarkdownContent(Map<String, Object> body) {
        Object mc = body.get("markdown_content");
        if (mc == null || !(mc instanceof CharSequence)) return null;
        String content = mc.toString();
        if (content.isBlank()) return null;
        // 匹配 ## 今日头条 或 ## 📰 今日头条 等
        Pattern p = Pattern.compile("(?m)^##\\s*(?:📰\\s*)?今日头条\\s*$");
        Matcher m = p.matcher(content);
        if (!m.find()) return null;
        int start = m.end();
        int end = content.length();
        Matcher next = Pattern.compile("(?m)^##\\s+").matcher(content);
        if (next.find(start)) end = next.start();
        return content.substring(start, end).trim();
    }

    /** 从整棵 body 中递归补齐四个平台的 copy，返回补全后的 map（可能部分仍为空） */
    private Map<String, String> fillPlatformCopiesFromBody(Map<String, Object> body, Set<String> platformFilter) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < PLATFORM_KEYS.length; i++) {
            String key = PLATFORM_KEYS[i];
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            String copy = (body instanceof Map) ? findPlatformCopyRecursively((Map<String, Object>) body, PLATFORM_SEARCH_KEYS[i]) : null;
            if (copy != null && !copy.isEmpty()) out.put(key, copy);
        }
        return out;
    }

    /**
     * 官方 stream_run 文档：workflow_end 事件中 data.output 为工作流整体输出，含 final_result。
     * 格式：{"id":0,"event":"message","data":{"type":"workflow_end","output":{"final_result":{...}}}}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractWorkflowEndOutput(Map<String, Object> obj) {
        Object data = obj.get("data");
        if (data instanceof String) {
            Map<String, Object> dataMap = parseJsonMap((String) data);
            if (dataMap == null) return null;
            data = dataMap;
        }
        if (!(data instanceof Map)) return null;
        Map<String, Object> dataMap = (Map<String, Object>) data;
        if (!"workflow_end".equals(dataMap.get("type"))) return null;
        Object output = dataMap.get("output");
        if (output instanceof String) {
            Map<String, Object> outputMap = parseJsonMap((String) output);
            if (outputMap == null) return null;
            output = outputMap;
        }
        if (!(output instanceof Map)) return null;
        Map<String, Object> outputMap = (Map<String, Object>) output;
        Object fr = outputMap.get("final_result");
        boolean hasFinalResult = fr instanceof Map && (isPlatformResultMap((Map<?, ?>) fr) || !((Map<?, ?>) fr).isEmpty());
        boolean hasPlatformMarkdowns = outputMap.get("platform_markdowns") instanceof Map;
        if (hasFinalResult || hasPlatformMarkdowns) return outputMap;
        return null;
    }

    /** 将 withFinal 合并进 accumulated：final_result 与 platform_markdowns 按 key 合并，避免后一帧覆盖前一帧导致今日头条丢失 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeBodyInto(Map<String, Object> accumulated, Map<String, Object> withFinal) {
        if (withFinal == null) return accumulated;
        if (accumulated == null) {
            Map<String, Object> copy = new LinkedHashMap<>(withFinal);
            Object fr = withFinal.get("final_result");
            if (fr instanceof Map) copy.put("final_result", new LinkedHashMap<>((Map<?, ?>) fr));
            Object pm = withFinal.get("platform_markdowns");
            if (pm instanceof Map) copy.put("platform_markdowns", new LinkedHashMap<>((Map<?, ?>) pm));
            return copy;
        }
        Object frNew = withFinal.get("final_result");
        if (frNew instanceof Map) {
            Object frAcc = accumulated.get("final_result");
            if (!(frAcc instanceof Map)) accumulated.put("final_result", new LinkedHashMap<>((Map<?, ?>) frNew));
            else {
                Map<String, Object> accFr = (Map<String, Object>) frAcc;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) frNew).entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) accFr.put(e.getKey().toString(), e.getValue());
                }
            }
        }
        Object pmNew = withFinal.get("platform_markdowns");
        if (pmNew instanceof Map) {
            Object pmAcc = accumulated.get("platform_markdowns");
            if (!(pmAcc instanceof Map)) accumulated.put("platform_markdowns", new LinkedHashMap<>((Map<?, ?>) pmNew));
            else {
                Map<String, Object> accPm = (Map<String, Object>) pmAcc;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) pmNew).entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) accPm.put(e.getKey().toString(), e.getValue());
                }
            }
        }
        if (withFinal.get("markdown_content") instanceof CharSequence) {
            String mc = withFinal.get("markdown_content").toString().trim();
            if (!mc.isEmpty() && (!(accumulated.get("markdown_content") instanceof CharSequence) || accumulated.get("markdown_content").toString().trim().isEmpty())) {
                accumulated.put("markdown_content", mc);
            }
        }
        return accumulated;
    }

    /** 若 body 中仍无今日头条，从流式累积的 content 里解析【今日头条】段落或 JSON 中的 toutiao 并写入 body */
    private void injectToutiaoFromContentIfMissing(Map<String, Object> body, String contentStr) {
        if (body == null) return;
        if (hasToutiaoInBody(body)) return;
        if (contentStr == null || contentStr.isBlank()) return;
        String toutiao = extractToutiaoSectionFromContentString(contentStr);
        if ((toutiao == null || toutiao.isBlank()) && contentStr.trim().startsWith("{")) {
            Map<String, Object> parsed = parseJsonMap(contentStr.trim());
            if (parsed != null) {
                toutiao = findToutiaoCopyRecursively(parsed);
                if (toutiao == null || toutiao.isBlank()) toutiao = findToutiaoMarkdownInTree(parsed);
            }
        }
        if (toutiao != null && !toutiao.isBlank()) putToutiaoIntoBody(body, toutiao);
    }

    /** 从任意一条解析后的 SSE 对象中递归查找今日头条并写入 body（Coze 可能把 toutiao 放在与 output 平级或其它层级） */
    @SuppressWarnings("unchecked")
    private void tryInjectToutiaoFromParsedObject(Map<String, Object> body, Map<String, Object> parsed) {
        if (body == null || parsed == null || hasToutiaoInBody(body)) return;
        String copy = findToutiaoCopyRecursively(parsed);
        if (copy == null || copy.isBlank()) copy = findToutiaoMarkdownInTree(parsed);
        if (copy != null && !copy.isBlank()) putToutiaoIntoBody(body, copy);
    }

    private boolean hasToutiaoInBody(Map<String, Object> body) {
        Object fr = body.get("final_result");
        if (fr instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) fr;
            if (m.containsKey("toutiao") || m.containsKey("今日头条")) return true;
        }
        if (body.get("toutiao_copy") instanceof CharSequence && ((CharSequence) body.get("toutiao_copy")).length() > 0) return true;
        Object pm = body.get("platform_markdowns");
        if (pm instanceof Map && ((Map<?, ?>) pm).get("toutiao") instanceof CharSequence) return true;
        return false;
    }

    private void putToutiaoIntoBody(Map<String, Object> body, String toutiao) {
        Object fr = body.get("final_result");
        if (fr instanceof Map) {
            Map<String, Object> frMap = (Map<String, Object>) fr;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("platform", "今日头条");
            entry.put("copy", toutiao);
            frMap.put("toutiao", entry);
        } else {
            body.put("toutiao_copy", toutiao);
        }
    }

    /** 从整段 content 中截取今日头条段落，支持【今日头条】、## 今日头条、今日头条： 等格式 */
    private String extractToutiaoSectionFromContentString(String content) {
        if (content == null || content.isBlank()) return null;
        String[] markers = {"【今日头条】", "## 今日头条", "## 📰 今日头条", "今日头条：", "\n今日头条："};
        for (String marker : markers) {
            int start = content.indexOf(marker);
            if (start == -1) continue;
            int textStart = start + marker.length();
            int end = content.length();
            String[] others = {"【抖音】", "【小红书】", "【视频号】", "## 抖音", "## 小红书", "## 视频号"};
            for (String m : others) {
                int next = content.indexOf(m, textStart);
                if (next != -1 && next < end) end = next;
            }
            String segment = content.substring(textStart, end).trim();
            if (!segment.isEmpty()) return segment;
        }
        return null;
    }

    /** 从整棵 obj 中取出 platform_markdowns、markdown_content 并入 withFinal，确保 body 含 MD 兜底（Coze 可能把二者放在 output 兄弟节点） */
    @SuppressWarnings("unchecked")
    private void enrichBodyWithMarkdownFromTree(Map<String, Object> obj, Map<String, Object> withFinal) {
        if (obj == null || withFinal == null) return;
        Object pm = obj.get("platform_markdowns");
        if (pm instanceof Map && !withFinal.containsKey("platform_markdowns")) {
            withFinal.put("platform_markdowns", pm);
        }
        Object mc = obj.get("markdown_content");
        if (mc instanceof CharSequence && ((CharSequence) mc).length() > 0 && !withFinal.containsKey("markdown_content")) {
            withFinal.put("markdown_content", mc.toString());
        }
        for (String key : new String[]{"data", "output", "result", "payload"}) {
            Object val = obj.get(key);
            if (val instanceof Map) {
                enrichBodyWithMarkdownFromTree((Map<String, Object>) val, withFinal);
            } else if (val instanceof String) {
                Map<String, Object> parsed = parseJsonMap((String) val);
                if (parsed != null) enrichBodyWithMarkdownFromTree(parsed, withFinal);
            }
        }
    }

    /** Coze stream 可能把 final_result 放在 data/output/result 的 Map 或字符串里，统一从这里取出用于 lastWithFinal */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findMapContainingFinalResult(Map<String, Object> obj) {
        if (obj == null) return null;
        if (obj.containsKey("final_result")) {
            Object fr = obj.get("final_result");
            if (fr instanceof Map && isPlatformResultMap((Map<?, ?>) fr)) return obj;
        }
        // Coze 云端 stream_run 可能为扁平结构：{ type, run_id, output, time_cost_ms }，无 data 包装；output 为 Map 或 JSON 字符串
        Object outputVal = obj.get("output");
        if (outputVal != null) {
            Map<String, Object> outputMap = null;
            if (outputVal instanceof Map) {
                outputMap = (Map<String, Object>) outputVal;
            } else if (outputVal instanceof String) {
                String raw = ((String) outputVal).trim();
                if (!raw.isEmpty()) {
                    outputMap = parseJsonMap(raw);
                    if (outputMap == null) {
                        log.warn("Coze output 为字符串且解析失败，长度={}，前100字符: {}", raw.length(), raw.length() > 100 ? raw.substring(0, 100) + "..." : raw);
                    }
                }
            }
            if (outputMap != null && outputMap.containsKey("final_result") && outputMap.get("final_result") instanceof Map) {
                Map<?, ?> frMap = (Map<?, ?>) outputMap.get("final_result");
                if (isPlatformResultMap(frMap) || !frMap.isEmpty()) {
                    if (log.isDebugEnabled()) log.debug("从 obj.output 识别到 final_result, keys={}", frMap.keySet());
                    return outputMap;
                }
            }
            if (outputMap != null) {
                Map<String, Object> deeper = findMapContainingFinalResult(outputMap);
                if (deeper != null) return deeper;
            }
        }
        for (String key : new String[]{"data", "output", "result", "payload"}) {
            Object val = obj.get(key);
            if (val instanceof Map) {
                Map<String, Object> inner = (Map<String, Object>) val;
                if (inner.containsKey("final_result") && inner.get("final_result") instanceof Map && isPlatformResultMap((Map<?, ?>) inner.get("final_result")))
                    return inner;
                Map<String, Object> deeper = findMapContainingFinalResult(inner);
                if (deeper != null) return deeper;
            } else if (val instanceof String) {
                Map<String, Object> parsed = parseJsonMap((String) val);
                if (parsed != null) {
                    Map<String, Object> withFinal = findMapContainingFinalResult(parsed);
                    if (withFinal != null) return withFinal;
                }
            }
        }
        return null;
    }

    private Map<String, Object> parseJsonMap(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return null;
        try {
            return objectMapper.readValue(jsonStr.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /** 从单条 data 中提取 content：支持 content / message.content / data.content / text / result；output 为 Map 时不当作 content 拼入，避免覆盖 final_result 解析 */
    private Object extractContentFromData(Map<String, Object> obj) {
        Object c = obj.get("content");
        if (c != null) return c;
        Object msg = obj.get("message");
        if (msg instanceof Map) {
            c = ((Map<?, ?>) msg).get("content");
            if (c != null) return c;
        }
        Object data = obj.get("data");
        if (data instanceof Map) {
            c = ((Map<?, ?>) data).get("content");
            if (c != null) return c;
        }
        c = obj.get("text");
        if (c != null) return c;
        c = obj.get("result");
        if (c != null) return c;
        Object output = obj.get("output");
        if (output instanceof CharSequence) return output;
        return null;
    }

    /** 从任意 Map 中尽量取出一段可展示的文本（用于无 content 时的兜底） */
    @SuppressWarnings("unchecked")
    private String extractAnyTextFromMap(Map<String, Object> map) {
        for (String key : new String[]{"content", "text", "result", "output", "message", "body"}) {
            Object v = map.get(key);
            if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return v.toString();
            if (v instanceof Map) {
                String nested = extractAnyTextFromMap((Map<String, Object>) v);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    /** 从环境变量读取 Coze 鉴权 Key（云端工作流调用必需） */
    private String getCozeApiKey() {
        String key = System.getenv("COZE_WORKLOAD_IDENTITY_API_KEY");
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    /**
     * 连接失败等异常转为前端可读的提示
     */
    private String toUserFriendlyError(Exception e) {
        if (e instanceof HttpClientErrorException.Unauthorized) {
            return "Coze 鉴权失败(401)。请设置环境变量 COZE_WORKLOAD_IDENTITY_API_KEY（Coze 工作台 API Token）";
        }
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        if (e instanceof ResourceAccessException || cause instanceof ConnectException) {
            return "无法连接 Coze 工作流，请检查 base-url 配置及网络，或联系管理员";
        }
        String msg = e.getMessage();
        return msg != null && !msg.isEmpty() ? msg : e.getClass().getSimpleName();
    }

    /**
     * 从 body 中解析 final_result，支持嵌套（如 data.final_result、value.final_result）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> resolveFinalResultFromBody(Map<String, Object> body) {
        Object fr = body.get("final_result");
        if (fr instanceof Map) {
            Map<String, Object> frMap = (Map<String, Object>) fr;
            if (isPlatformResultMap(frMap)) {
                Map<String, Map<String, Object>> out = new LinkedHashMap<>();
                for (String key : PLATFORM_KEYS) {
                    Map<String, Object> v = getPlatformEntry(frMap, key);
                    if (v != null) out.put(key, v);
                }
                if (!out.isEmpty()) return out;
            }
            for (String nestedKey : new String[]{"value", "data", "result", "output"}) {
                Object inner = frMap.get(nestedKey);
                if (inner instanceof Map && isPlatformResultMap((Map<?, ?>) inner)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> innerMap = (Map<String, Object>) inner;
                    Map<String, Map<String, Object>> out = new LinkedHashMap<>();
                    for (String key : PLATFORM_KEYS) {
                        Map<String, Object> v = getPlatformEntry(innerMap, key);
                        if (v != null) out.put(key, v);
                    }
                    if (!out.isEmpty()) return out;
                }
            }
        }
        Object data = body.get("data");
        if (data instanceof Map) {
            Map<String, Map<String, Object>> fromData = resolveFinalResultFromBody((Map<String, Object>) data);
            if (fromData != null) return fromData;
        }
        if (data instanceof String) {
            Map<String, Object> dataMap = parseJsonMap((String) data);
            if (dataMap != null) {
                Map<String, Map<String, Object>> fromData = resolveFinalResultFromBody(dataMap);
                if (fromData != null) return fromData;
            }
        }
        for (String key : new String[]{"result", "output", "payload", "data"}) {
            Object val = body.get(key);
            if (val instanceof Map) {
                Map<String, Map<String, Object>> fromInner = resolveFinalResultFromBody((Map<String, Object>) val);
                if (fromInner != null) return fromInner;
            }
            if (val instanceof String) {
                Map<String, Object> inner = parseJsonMap((String) val);
                if (inner != null) {
                    Map<String, Map<String, Object>> fromInner = resolveFinalResultFromBody(inner);
                    if (fromInner != null) return fromInner;
                }
            }
        }
        // Coze 可能直接返回 state 顶层字段：douyin_copy, xiaohongshu_copy, video_account_copy, toutiao_copy
        Map<String, Map<String, Object>> fromTopLevel = buildFinalResultFromTopLevelCopies(body);
        if (fromTopLevel != null) return fromTopLevel;
        return null;
    }

    /** 从 body 顶层 copy 字段构建 final_result（与 merge_result 输出一致，确保今日头条能入库） */
    private Map<String, Map<String, Object>> buildFinalResultFromTopLevelCopies(Map<String, Object> body) {
        Object douyin = firstNonBlank(body, "douyin_copy", "douyinCopy");
        Object xiaohongshu = firstNonBlank(body, "xiaohongshu_copy", "xiaohongshuCopy");
        Object videoAccount = firstNonBlank(body, "video_account_copy", "videoAccountCopy");
        Object toutiao = firstNonBlank(body, "toutiao_copy", "toutiaoCopy");
        if (douyin == null && xiaohongshu == null && videoAccount == null && toutiao == null) return null;
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        if (douyin != null && douyin.toString().trim().length() > 0) {
            Map<String, Object> e = new LinkedHashMap<>(); e.put("platform", "抖音"); e.put("copy", douyin.toString().trim()); out.put("douyin", e);
        }
        if (xiaohongshu != null && xiaohongshu.toString().trim().length() > 0) {
            Map<String, Object> e = new LinkedHashMap<>(); e.put("platform", "小红书"); e.put("copy", xiaohongshu.toString().trim()); out.put("xiaohongshu", e);
        }
        if (videoAccount != null && videoAccount.toString().trim().length() > 0) {
            Map<String, Object> e = new LinkedHashMap<>(); e.put("platform", "视频号"); e.put("copy", videoAccount.toString().trim()); out.put("video_account", e);
        }
        if (toutiao != null && toutiao.toString().trim().length() > 0) {
            Map<String, Object> e = new LinkedHashMap<>(); e.put("platform", "今日头条"); e.put("copy", toutiao.toString().trim()); out.put("toutiao", e);
        }
        return out.isEmpty() ? null : out;
    }

    private boolean isPlatformResultMap(Map<?, ?> map) {
        for (String k : PLATFORM_KEYS) {
            if (map.containsKey(k)) return true;
        }
        if (map.containsKey("今日头条") || map.containsKey("抖音") || map.containsKey("小红书") || map.containsKey("视频号")) return true;
        return false;
    }

    /** 今日头条在 final_result 中可能的 key（Coze 不同发布方式可能不同） */
    private static final String[] TOUTIAO_KEYS = {"toutiao", "今日头条", "Toutiao", "toutiao_copy", "toutiaoCopy"};

    /** 从 final_result 子 Map 中取某平台条目，兼容英文/中文 key，且值可为 Map 或直接为文案字符串 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getPlatformEntry(Map<String, ?> frMap, String platformKey) {
        Object v = frMap.get(platformKey);
        if (v instanceof Map) return normalizePlatformEntry((Map<String, Object>) v, platformKey);
        if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return buildEntry(platformKey, v.toString().trim());
        if ("toutiao".equals(platformKey)) {
            for (String k : TOUTIAO_KEYS) {
                if (k.equals(platformKey)) continue;
                v = frMap.get(k);
                if (v instanceof Map) return normalizePlatformEntry((Map<String, Object>) v, platformKey);
                if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return buildEntry("toutiao", v.toString().trim());
            }
        }
        if ("douyin".equals(platformKey)) { v = frMap.get("抖音"); if (v instanceof Map) return normalizePlatformEntry((Map<String, Object>) v, platformKey); if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return buildEntry("douyin", v.toString().trim()); }
        if ("xiaohongshu".equals(platformKey)) { v = frMap.get("小红书"); if (v instanceof Map) return normalizePlatformEntry((Map<String, Object>) v, platformKey); if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return buildEntry("xiaohongshu", v.toString().trim()); }
        if ("video_account".equals(platformKey)) { v = frMap.get("视频号"); if (v instanceof Map) return normalizePlatformEntry((Map<String, Object>) v, platformKey); if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return buildEntry("video_account", v.toString().trim()); }
        return null;
    }

    private Map<String, Object> buildEntry(String platformKey, String copy) {
        String label = PLATFORM_LABELS.get(platformKey);
        if (label == null) label = platformKey;
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("platform", label);
        e.put("copy", copy);
        return e;
    }

    /** Map 形式的平台条目：优先 copy，无则用 content，保证统一为 { platform, copy } */
    private Map<String, Object> normalizePlatformEntry(Map<String, Object> entry, String platformKey) {
        Object copy = entry.get("copy");
        if (copy == null || copy.toString().trim().isEmpty()) copy = entry.get("content");
        if (copy == null || copy.toString().trim().isEmpty()) return null;
        String label = PLATFORM_LABELS.get(platformKey);
        if (label == null) label = (String) entry.get("platform");
        if (label == null) label = platformKey;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("platform", label);
        out.put("copy", copy.toString().trim());
        return out;
    }

    /**
     * 从 Coze 返回的 content 字符串中解析出 final_result 结构。
     * 支持：1) 整串为 JSON（含 final_result）；2) Java Map 风格字符串；3) 【平台】段落格式。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseFinalResultFromContentString(String contentStr) {
        if (contentStr == null || contentStr.isBlank()) return null;
        // 1) 尝试按 JSON 解析（部署站点可能返回 JSON 字符串）
        try {
            Map<String, Object> root = objectMapper.readValue(contentStr, new TypeReference<Map<String, Object>>() {});
            Object fr = root.get("final_result");
            if (fr instanceof Map) {
                Map<String, Object> frMap = (Map<String, Object>) fr;
                Map<String, Map<String, Object>> out = new LinkedHashMap<>();
                for (String key : PLATFORM_KEYS) {
                    Map<String, Object> v = getPlatformEntry(frMap, key);
                    if (v != null) out.put(key, v);
                }
                if (!out.isEmpty()) return out;
            }
        } catch (Exception e) {
            // 非 JSON，继续尝试 Map 风格
        }
        // 2) Java Map 风格：final_result={douyin={platform=抖音, copy=...}, ...}，任一平台存在即尝试解析（与抖音/小红书逻辑一致复用到视频号/今日头条）
        boolean hasAnyPlatform = contentStr.contains("platform=抖音") || contentStr.contains("platform=小红书")
                || contentStr.contains("platform=视频号") || contentStr.contains("platform=今日头条");
        if (!hasAnyPlatform) {
            return null;
        }
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        String[] platformKeys = PLATFORM_KEYS;
        String[] platformLabels = {"抖音", "小红书", "视频号", "今日头条"};
        for (int i = 0; i < platformKeys.length; i++) {
            String key = platformKeys[i];
            String label = platformLabels[i];
            // 结束位置：下一个平台 "}, xxx={" 或 final_result 结束 "}}"
            StringBuilder sb = new StringBuilder("(?:");
            for (int j = i + 1; j < platformKeys.length; j++) {
                if (j > i + 1) sb.append("|");
                sb.append("\\}, ").append(platformKeys[j]).append("=\\{");
            }
            sb.append("|\\}\\})");
            String endPattern = sb.toString();
            Pattern p = Pattern.compile(
                    Pattern.quote(key + "={platform=" + label + ", copy=") + "(.*?)" + endPattern,
                    Pattern.DOTALL);
            Matcher m = p.matcher(contentStr);
            if (m.find()) {
                String copy = m.group(1).trim();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("platform", label);
                entry.put("copy", copy);
                out.put(key, entry);
            }
        }
        if (!out.isEmpty()) return out;
        // 3) 【平台】段落格式：【视频号】\n...\n\n【今日头条】\n...（Coze 流式只返回 content 时用此解析，确保今日头条能入库）
        if (contentStr.contains("【") && contentStr.contains("】")) {
            Map<String, Map<String, Object>> outFromMarkers = buildFinalResultFromPlatformMarkers(contentStr);
            if (outFromMarkers != null && !outFromMarkers.isEmpty()) return outFromMarkers;
        }
        return null;
    }

    /** 从含【平台名】段落的 content 中解析出 final_result 结构，保证今日头条等平台能入库 */
    private Map<String, Map<String, Object>> buildFinalResultFromPlatformMarkers(String fullContent) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        String[] platformLabels = {"抖音", "小红书", "视频号", "今日头条"};
        for (int i = 0; i < PLATFORM_KEYS.length; i++) {
            String key = PLATFORM_KEYS[i];
            String label = platformLabels[i];
            String markerBracket = "【" + label + "】";
            String markerColon = label + "：";
            int start = -1;
            int markerLen = 0;
            if (fullContent.contains(markerBracket)) {
                start = fullContent.indexOf(markerBracket);
                markerLen = markerBracket.length();
            } else if (fullContent.contains(markerColon)) {
                start = fullContent.indexOf(markerColon);
                markerLen = markerColon.length();
            } else if (fullContent.contains("\n" + markerColon)) {
                start = fullContent.indexOf("\n" + markerColon) + 1;
                markerLen = markerColon.length();
            }
            if (start < 0) continue;
            start += markerLen;
            int end = fullContent.length();
            for (String other : platformLabels) {
                if (other.equals(label)) continue;
                int next = fullContent.indexOf("【" + other + "】", start);
                if (next >= 0 && next < end) end = next;
                next = fullContent.indexOf("\n" + other + "：", start);
                if (next >= 0 && next < end) end = next;
            }
            String copy = fullContent.substring(start, end).trim();
            if (copy.isEmpty()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("platform", label);
            entry.put("copy", copy);
            out.put(key, entry);
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * 从 final_result 中按平台提取 copy 文案，用于入库；若 platformFilter 非空则只取选中平台。
     */
    private Map<String, String> extractPlatformCopies(Map<String, Map<String, Object>> finalResult, Set<String> platformFilter) {
        Map<String, String> out = new HashMap<>();
        for (String key : PLATFORM_KEYS) {
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            Map<String, Object> platform = getPlatformEntry(finalResult, key);
            if (platform == null) continue;
            Object copy = platform.get("copy");
            if (copy != null) out.put(key, copy.toString().trim());
        }
        return out.isEmpty() ? null : out;
    }

    /** 平台 key 与前端展示名（【】内）对应，用于从整段 content 中按段过滤 */
    private static final Map<String, String> PLATFORM_LABELS = new LinkedHashMap<String, String>() {{
        put("douyin", "抖音");
        put("xiaohongshu", "小红书");
        put("video_account", "视频号");
        put("toutiao", "今日头条");
    }};

    /**
     * 当 Coze 只返回整段 content（无 final_result）时，按选中平台过滤。支持【平台名】或 平台名： 两种段落标题。
     */
    private String filterContentByPlatforms(String fullContent, Set<String> platformFilter) {
        if (platformFilter == null || platformFilter.isEmpty()) return fullContent;
        StringBuilder sb = new StringBuilder();
        for (String key : PLATFORM_KEYS) {
            if (!platformFilter.contains(key)) continue;
            String label = PLATFORM_LABELS.get(key);
            if (label == null) continue;
            int start = -1;
            int markerLen = 0;
            String markerBracket = "【" + label + "】";
            String markerColon = label + "：";
            if (fullContent.contains(markerBracket)) {
                start = fullContent.indexOf(markerBracket);
                markerLen = markerBracket.length();
            } else if (fullContent.contains(markerColon)) {
                start = fullContent.indexOf(markerColon);
                markerLen = markerColon.length();
            } else if (fullContent.contains("\n" + markerColon)) {
                start = fullContent.indexOf("\n" + markerColon) + 1;
                markerLen = markerColon.length();
            }
            if (start < 0) continue;
            start += markerLen;
            int end = fullContent.length();
            for (String other : PLATFORM_LABELS.values()) {
                if (other.equals(label)) continue;
                int next = fullContent.indexOf("【" + other + "】", start);
                if (next >= 0 && next < end) end = next;
                next = fullContent.indexOf("\n" + other + "：", start);
                if (next >= 0 && next < end) end = next;
            }
            String segment = fullContent.substring(start, end).trim();
            if (segment.length() > 0) {
                sb.append("【").append(label).append("】\n").append(segment).append("\n\n");
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : fullContent;
    }

    /**
     * 从整段 content 中解析出各平台段落，用于入库（douyin_copy 等）。若 platformFilter 非空则只返回选中平台。
     */
    private Map<String, String> extractPlatformSectionsFromContent(String fullContent, Set<String> platformFilter) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : PLATFORM_KEYS) {
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            String label = PLATFORM_LABELS.get(key);
            if (label == null) continue;
            int start = -1;
            int markerLen = 0;
            String markerBracket = "【" + label + "】";
            String markerColon = label + "：";
            if (fullContent.contains(markerBracket)) {
                start = fullContent.indexOf(markerBracket);
                markerLen = markerBracket.length();
            } else if (fullContent.contains(markerColon)) {
                start = fullContent.indexOf(markerColon);
                markerLen = markerColon.length();
            } else if (fullContent.contains("\n" + markerColon)) {
                start = fullContent.indexOf("\n" + markerColon) + 1;
                markerLen = markerColon.length();
            }
            if (start < 0) continue;
            start += markerLen;
            int end = fullContent.length();
            for (String other : PLATFORM_LABELS.values()) {
                if (other.equals(label)) continue;
                int next = fullContent.indexOf("【" + other + "】", start);
                if (next >= 0 && next < end) end = next;
                next = fullContent.indexOf("\n" + other + "：", start);
                if (next >= 0 && next < end) end = next;
            }
            String segment = fullContent.substring(start, end).trim();
            if (segment.length() > 0) out.put(key, segment);
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * 将 Coze 返回的 final_result 格式化为前端可展示的文案；若 platformFilter 非空则只包含选中平台。
     */
    private String formatFinalResult(Map<String, Map<String, Object>> finalResult, Set<String> platformFilter) {
        StringBuilder sb = new StringBuilder();
        for (String key : PLATFORM_KEYS) {
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            Map<String, Object> platform = getPlatformEntry(finalResult, key);
            if (platform == null) continue;
            String platformName = platform.get("platform") != null ? platform.get("platform").toString() : key;
            Object copy = platform.get("copy");
            sb.append("【").append(platformName).append("】\n");
            if (copy != null) sb.append(copy.toString().trim());
            sb.append("\n\n");
        }
        return sb.toString().trim();
    }

    /** 从 Map<平台key, copy文案> 格式化为【平台名】+ 文案，用于合并递归结果后的统一展示 */
    private String formatFromPlatformCopies(Map<String, String> platformCopies, Set<String> platformFilter) {
        if (platformCopies == null || platformCopies.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String key : PLATFORM_KEYS) {
            if (platformFilter != null && !platformFilter.contains(key)) continue;
            String copy = platformCopies.get(key);
            if (copy == null || copy.trim().isEmpty()) continue;
            String label = PLATFORM_LABELS.get(key);
            if (label == null) label = key;
            sb.append("【").append(label).append("】\n").append(copy.trim()).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Transactional(rollbackFor = Exception.class)
    private void saveResult(Long taskId, String reasoning, String content, String error, Map<String, String> platformCopies) {
        GenerationTask task = new GenerationTask();
        task.setId(taskId);
        task.setReasoning(reasoning != null ? reasoning : "");
        task.setContent(content != null ? content : "");
        if (platformCopies != null) {
            task.setDouyinCopy(platformCopies.get("douyin"));
            task.setXiaohongshuCopy(platformCopies.get("xiaohongshu"));
            task.setVideoAccountCopy(platformCopies.get("video_account"));
            task.setToutiaoCopy(platformCopies.get("toutiao"));
        }
        task.setStatus(error == null ? 1 : 2);
        task.setWordCount(task.getContent().length());
        task.setCompletedAt(LocalDateTime.now());

        if (log.isInfoEnabled() && platformCopies != null) {
            log.info("saveResult taskId={} platforms={} toutiaoPresent={} toutiaoLen={}", taskId, platformCopies.keySet(),
                    platformCopies.containsKey("toutiao"), platformCopies.get("toutiao") != null ? platformCopies.get("toutiao").length() : 0);
        }

        try {
            int updated = generationMapper.updateResult(task);
            if (updated > 0) {
                log.debug("文案已入库 taskId={} contentLen={} platforms={}", taskId, task.getContent().length(), platformCopies != null ? platformCopies.keySet() : "all");
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            Throwable c = e.getCause();
            while (c != null) {
                msg = msg != null ? msg + " " + c.getMessage() : c.getMessage();
                c = c.getCause();
            }
            log.warn("ai_generation 全量更新失败，改用仅 content 更新。请执行 migration_add_platform_copy_columns.sql。err={}", msg);
            int basic = generationMapper.updateResultBasic(task);
            if (basic > 0) {
                log.info("文案已以基础字段入库 taskId={}", taskId);
            } else {
                log.error("文案入库失败 taskId={}", taskId, e);
                errorLogService.logAndMessage("generate", "入库", "ai_generation 更新失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e, "taskId=" + taskId);
                throw e;
            }
        }
    }

    public GenerationTask getTask(Long id) {
        return generationMapper.selectById(id);
    }
}
