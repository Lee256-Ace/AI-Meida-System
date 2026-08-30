package com.product.service;

import com.product.entity.PolishTask;
import com.product.mapper.PolishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolishService {

    private final PolishMapper polishMapper;
    private final QwenWorkflowClient qwenWorkflowClient;
    private final ErrorLogService errorLogService;

    /** 美化类型 -> 美化目标：整体优化/增强文采/精简文字/调整语气 */
    private static final Map<String, String> BEAUTIFY_TARGET_MAP = new LinkedHashMap<>();
    static {
        BEAUTIFY_TARGET_MAP.put("simplify", "精简文字");
        BEAUTIFY_TARGET_MAP.put("enhance", "增强文采");
        BEAUTIFY_TARGET_MAP.put("grammar", "整体优化");
        BEAUTIFY_TARGET_MAP.put("rewrite", "调整语气");
    }

    public interface StreamHandler {
        void onContent(String text);
        void onComplete();
        void onError(String error);
    }

    @Transactional
    public Long createTask(String originalContent, String polishType, String targetStyle, String platform) {
        PolishTask task = new PolishTask();
        task.setOriginalContent(originalContent);
        task.setPolishType(polishType);
        task.setTargetStyle(targetStyle);
        task.setPlatform(platform);
        task.setModel("qwen");
        task.setStatus(0);
        task.setCreatedAt(LocalDateTime.now());
        polishMapper.insert(task);
        return task.getId();
    }

    @Async
    public void processTask(Long taskId, String originalContent, String polishType, String targetStyle, String platform, StreamHandler handler) {
        if (originalContent == null || originalContent.isBlank()) {
            String msg = errorLogService.logAndMessage("polish", "参数校验", "文章内容为空", null, "taskId=" + taskId);
            if (handler != null) handler.onError(msg);
            return;
        }

        String systemPrompt = "你是一名专业的中文文章编辑，擅长对文章进行润色美化。仅输出美化后的文章全文，不要输出任何解释、前缀、说明或标题。";
        // 官方默认：beautify_target 整体优化，target_audience 大众
        String beautifyTarget = BEAUTIFY_TARGET_MAP.getOrDefault(polishType != null ? polishType : "", "整体优化");
        String audience = (targetStyle != null && !targetStyle.isBlank()) ? targetStyle.trim() : "大众";
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请对以下文章进行").append(beautifyTarget).append("。\n");
        userPrompt.append("目标读者：").append(audience).append("\n\n");
        userPrompt.append("文章原文：\n").append(originalContent.trim()).append("\n\n");
        userPrompt.append("请输出美化后的完整文章。");

        qwenWorkflowClient.generateStream(systemPrompt, userPrompt.toString(), new QwenWorkflowClient.StreamHandler() {
            @Override
            public void onContent(String text) {
                if (handler != null) handler.onContent(text);
            }
            @Override
            public void onComplete(String fullText) {
                String content = fullText != null ? fullText.trim() : "";
                saveResult(taskId, content, platform, null);
                if (handler != null) handler.onComplete();
            }
            @Override
            public void onError(String message) {
                saveResult(taskId, "", platform, message);
                String display = errorLogService.logAndMessage("polish", "千问美化调用", message != null ? message : "美化失败", null, "taskId=" + taskId);
                if (handler != null) handler.onError(display);
            }
        });
    }

    private void saveResult(Long taskId, String resultContent, String platform, String error) {
        PolishTask task = new PolishTask();
        task.setId(taskId);
        task.setResultContent(resultContent);
        task.setPlatform(platform);
        task.setStatus(error == null ? 1 : 2);
        task.setCompletedAt(LocalDateTime.now());
        polishMapper.updateResult(task);
    }

    public PolishTask getTask(Long id) {
        return polishMapper.selectById(id);
    }
}
