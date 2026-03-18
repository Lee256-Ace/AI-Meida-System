package com.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一调用 Coze 云端 stream_run，供文案生成以外的模块（续写、美化）使用。
 * 流式读取 SSE，按内容片段回调 onContent，结束时回调 onComplete(finalBody)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CozeWorkflowClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ErrorLogService errorLogService;

    @Value("${coze.workflow.base-url:https://j7v23xgtbm.coze.site}")
    private String cozeBaseUrl;

    @Value("${coze.workflow.endpoint-path:/stream_run}")
    private String cozeEndpointPath;

    public interface StreamHandler {
        void onContent(String text);
        void onComplete(Map<String, Object> finalBody);
        void onError(String message);
    }

    /**
     * 调用 Coze stream_run，流式回调 onContent，结束时回调 onComplete(finalBody)。
     * finalBody 可能含 content、beautified_article、extended_article 等，由工作流决定。
     */
    public void postStreamRun(Map<String, Object> payload, StreamHandler handler) {
        String path = (cozeEndpointPath == null || cozeEndpointPath.isBlank()) ? "/stream_run" : cozeEndpointPath;
        if (!path.startsWith("/")) path = "/" + path;
        String url = cozeBaseUrl.replaceAll("/$", "") + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "text/event-stream");
        String apiKey = getCozeApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey.trim());
        }

        try {
            restTemplate.execute(url, HttpMethod.POST,
                request -> {
                    headers.forEach((k, v) -> request.getHeaders().addAll(k, v));
                    request.getBody().write(objectMapper.writeValueAsBytes(payload));
                },
                response -> {
                    if (response.getStatusCode() != HttpStatusCode.valueOf(200)) {
                        throw new RuntimeException("Coze stream_run 返回异常: " + response.getStatusCode());
                    }
                    Map<String, Object> lastBody = null;
                    StringBuilder contentAccum = new StringBuilder();
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
                            if (json == null || json.isEmpty() || "[DONE]".equalsIgnoreCase(json) || "DONE".equalsIgnoreCase(json)) {
                                continue;
                            }
                            try {
                                Map<String, Object> obj = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                                Map<String, Object> bodyFromLine = extractWorkflowEndOutput(obj);
                                if (bodyFromLine == null) bodyFromLine = extractBodyFromOutput(obj);
                                if (bodyFromLine != null) lastBody = bodyFromLine;
                                Object c = extractContentFromData(obj);
                                if (c != null) {
                                    String s = c.toString();
                                    contentAccum.append(s);
                                    if (handler != null) handler.onContent(s);
                                }
                            } catch (Exception e) {
                                // skip malformed line
                            }
                        }
                    }
                    if (lastBody == null && contentAccum.length() > 0) {
                        lastBody = new LinkedHashMap<>();
                        lastBody.put("content", contentAccum.toString());
                    }
                    if (lastBody != null && log.isDebugEnabled()) {
                        log.debug("Coze stream 结束 lastBody keys: {}", lastBody.keySet());
                    }
                    if (handler != null) handler.onComplete(lastBody != null ? lastBody : new LinkedHashMap<>());
                    return null;
                });
        } catch (Exception e) {
            log.error("Coze stream_run 调用失败", e);
            errorLogService.logAndMessage("coze", "stream_run", e.getMessage() != null ? e.getMessage() : "调用 Coze 工作流失败", e, null);
            if (handler != null) handler.onError(e.getMessage() != null ? e.getMessage() : "调用 Coze 工作流失败");
        }
    }

    private String getCozeApiKey() {
        String key = System.getenv("COZE_WORKLOAD_IDENTITY_API_KEY");
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractWorkflowEndOutput(Map<String, Object> obj) {
        Object data = obj.get("data");
        if (data instanceof String) {
            try {
                data = objectMapper.readValue((String) data, Map.class);
            } catch (Exception e) {
                return null;
            }
        }
        if (!(data instanceof Map)) return null;
        Map<String, Object> dataMap = (Map<String, Object>) data;
        if (!"workflow_end".equals(dataMap.get("type"))) return null;
        Object output = dataMap.get("output");
        if (output instanceof Map) return (Map<String, Object>) output;
        if (output instanceof String) {
            Map<String, Object> parsed = parseJsonMap((String) output);
            if (parsed != null) return parsed;
        }
        return null;
    }

    /** 扁平帧 { type, run_id, output, time_cost_ms }：output 为 Map 或 JSON 字符串，含 final_result / beautified_article / extended_article 等 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBodyFromOutput(Map<String, Object> obj) {
        if (obj == null) return null;
        Object outputVal = obj.get("output");
        if (outputVal instanceof Map) return (Map<String, Object>) outputVal;
        if (outputVal instanceof String) {
            String raw = ((String) outputVal).trim();
            if (!raw.isEmpty()) return parseJsonMap(raw);
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

    /** 只提取可流式展示的文本片段，避免把 output/final_result 等 Map 当 content 导致界面出现 {final_result=...} */
    private Object extractContentFromData(Map<String, Object> obj) {
        Object c = obj.get("content");
        if (c instanceof CharSequence) return c;
        if (c instanceof Map) return null;
        Object data = obj.get("data");
        if (data instanceof Map) {
            c = ((Map<?, ?>) data).get("content");
            if (c instanceof CharSequence) return c;
            if (c instanceof Map) return null;
        }
        c = obj.get("text");
        if (c instanceof CharSequence) return c;
        c = obj.get("result");
        if (c instanceof CharSequence) return c;
        c = obj.get("output");
        return (c instanceof CharSequence) ? c : null;
    }

    /**
     * 从 Coze 返回的 finalBody 中提取主文本，按 preferredKeys 顺序尝试。
     * 支持顶层字段，也支持 final_result 内的 beautified_article / extended_article / markdown_content 等。
     */
    @SuppressWarnings("unchecked")
    public static String extractMainText(Map<String, Object> finalBody, String... preferredKeys) {
        if (finalBody == null) return "";
        String fromTop = extractMainTextFromMap(finalBody, preferredKeys);
        if (fromTop != null && !fromTop.isEmpty()) return fromTop;
        Object fr = finalBody.get("final_result");
        if (fr instanceof Map) {
            String fromFr = extractMainTextFromMap((Map<String, Object>) fr, preferredKeys);
            if (fromFr != null && !fromFr.isEmpty()) return fromFr;
        }
        Object c = finalBody.get("content");
        return c != null ? c.toString().trim() : "";
    }

    private static String extractMainTextFromMap(Map<String, Object> map, String... preferredKeys) {
        if (map == null) return "";
        for (String key : preferredKeys) {
            Object v = map.get(key);
            if (v instanceof CharSequence && ((CharSequence) v).length() > 0) return v.toString().trim();
            if (v instanceof Map) {
                Object inner = ((Map<?, ?>) v).get("content");
                if (inner instanceof CharSequence && ((CharSequence) inner).length() > 0) return inner.toString().trim();
            }
        }
        Object c = map.get("content");
        return c != null ? c.toString().trim() : "";
    }

    /**
     * 从官方输出的 platform_markdowns 中取指定 key（如 article 表示文章处理 Markdown）。
     * 支持顶层或 final_result 内的 platform_markdowns。
     */
    @SuppressWarnings("unchecked")
    public static String extractFromPlatformMarkdowns(Map<String, Object> finalBody, String sectionKey) {
        if (finalBody == null || sectionKey == null) return "";
        Object pm = finalBody.get("platform_markdowns");
        if (pm instanceof Map) {
            Object section = ((Map<?, ?>) pm).get(sectionKey);
            if (section instanceof CharSequence && ((CharSequence) section).length() > 0) return section.toString().trim();
        }
        Object fr = finalBody.get("final_result");
        if (fr instanceof Map) {
            pm = ((Map<?, ?>) fr).get("platform_markdowns");
            if (pm instanceof Map) {
                Object section = ((Map<?, ?>) pm).get(sectionKey);
                if (section instanceof CharSequence && ((CharSequence) section).length() > 0) return section.toString().trim();
            }
        }
        return "";
    }
}
