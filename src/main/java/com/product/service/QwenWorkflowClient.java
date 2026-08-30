package com.product.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一调用阿里云百炼（通义千问）大模型，替代原 Coze 云端工作流。
 * 提供阻塞式 generate（返回完整文本）与流式 generateStream（按片段回调）两种能力，
 * 供文案生成、文章美化、文章续写模块使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QwenWorkflowClient {

    private final ErrorLogService errorLogService;

    /** 通义千问 API Key（按需求直接配置在代码中） */
    private static final String API_KEY = "sk-ws-H.EXIIYHL.twNw.MEQCIBcsbwzxcd_LVKN_hkK1uK2Dli1UpvVj0QJ8stQpuZm9AiAqk9Az0XJ8uXdyYz4zIx8XngwAl6xsPrl8VN06HUGZfA";

    /** 模型名称，可通过 application.yml 的 qwen.model 覆盖 */
    @Value("${qwen.model:qwen-plus}")
    private String model;

    public interface StreamHandler {
        /** 流式片段（可能多次回调） */
        void onContent(String text);
        /** 生成结束，返回完整文本 */
        void onComplete(String fullText);
        /** 生成失败 */
        void onError(String message);
    }

    /**
     * 阻塞式生成，返回完整文本；失败抛出 RuntimeException。
     */
    public String generate(String systemPrompt, String userPrompt) {
        try {
            Generation generation = new Generation();
            GenerationResult result = generation.call(buildParam(systemPrompt, userPrompt, false));
            return extractText(result);
        } catch (Exception e) {
            throw new RuntimeException(toMessage(e), e);
        }
    }

    /**
     * 流式生成：按片段回调 onContent，结束时回调 onComplete(全文)，异常回调 onError。
     */
    public void generateStream(String systemPrompt, String userPrompt, StreamHandler handler) {
        StringBuilder full = new StringBuilder();
        try {
            Generation generation = new Generation();
            GenerationParam param = buildParam(systemPrompt, userPrompt, true);
            generation.streamCall(param, new ResultCallback<GenerationResult>() {
                @Override
                public void onEvent(GenerationResult item) {
                    String text = extractText(item);
                    if (text != null && !text.isEmpty()) {
                        full.append(text);
                        if (handler != null) handler.onContent(text);
                    }
                }
                @Override
                public void onComplete() {
                    if (handler != null) handler.onComplete(full.toString());
                }
                @Override
                public void onError(Exception e) {
                    handleError("千问生成失败：" + toMessage(e), e, handler);
                }
            });
        } catch (Exception e) {
            handleError("千问生成失败：" + toMessage(e), e, handler);
        }
    }

    private GenerationParam buildParam(String systemPrompt, String userPrompt, boolean incremental) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
        }
        messages.add(Message.builder().role(Role.USER.getValue()).content(userPrompt).build());
        return GenerationParam.builder()
                .apiKey(API_KEY)
                .model(model)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(incremental)
                .build();
    }

    private String extractText(GenerationResult result) {
        try {
            if (result == null || result.getOutput() == null || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()) {
                return "";
            }
            String content = result.getOutput().getChoices().get(0).getMessage().getContent();
            return content != null ? content : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void handleError(String message, Throwable e, StreamHandler handler) {
        log.error(message, e);
        errorLogService.logAndMessage("qwen", "generate", message, e, null);
        if (handler != null) handler.onError(message);
    }

    private String toMessage(Throwable e) {
        return (e != null && e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage()
                : (e != null ? e.getClass().getSimpleName() : "未知错误");
    }
}