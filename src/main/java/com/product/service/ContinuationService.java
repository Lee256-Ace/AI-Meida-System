package com.product.service;

import com.product.entity.ContinuationTask;
import com.product.mapper.ContinuationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContinuationService {

    private final ContinuationMapper continuationMapper;
    private final CozeWorkflowClient cozeWorkflowClient;
    private final ErrorLogService errorLogService;

    @Transactional
    public Long createTask(String originalContent, String contextHint, String platform) {
        ContinuationTask task = new ContinuationTask();
        task.setOriginalContent(originalContent);
        task.setContextHint(contextHint);
        task.setPlatform(platform);
        task.setModel("coze-workflow");
        task.setStatus(0);
        task.setCreatedAt(LocalDateTime.now());
        continuationMapper.insert(task);
        return task.getId();
    }

    /**
     * 续写：调用 Coze 工作流 operations=["extend"]，支持 extend_length、extend_style。
     * extend_length: 短篇/中等/长篇；extend_style: 保持原风格/更正式/更生动。
     */
    @Async
    public void processTask(Long taskId, String originalContent, String contextHint, String platform,
                           String extendLength, String extendStyle, StreamHandler handler) {
        if (originalContent == null || originalContent.isBlank()) {
            String msg = errorLogService.logAndMessage("continue", "参数校验", "文章内容为空", null, "taskId=" + taskId);
            if (handler != null) handler.onError(msg);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operations", List.of("extend"));
        payload.put("article_content", originalContent.trim());
        payload.put("extend_length", extendLength != null && !extendLength.isBlank() ? extendLength.trim() : "中等");
        payload.put("extend_style", extendStyle != null && !extendStyle.isBlank() ? extendStyle.trim() : "保持原风格");
        if (contextHint != null && !contextHint.isBlank()) {
            payload.put("context_hint", contextHint.trim());
        }

        cozeWorkflowClient.postStreamRun(payload, new CozeWorkflowClient.StreamHandler() {
            @Override
            public void onContent(String text) {
                if (handler != null) handler.onContent(text);
            }
            @Override
            public void onComplete(Map<String, Object> finalBody) {
                // 官方输出优先：markdown_content、platform_markdowns.article，其次 extended_article
                String continuation = CozeWorkflowClient.extractMainText(finalBody, "markdown_content", "extended_article", "content");
                if (continuation.isEmpty()) continuation = CozeWorkflowClient.extractFromPlatformMarkdowns(finalBody, "article");
                if (continuation.isEmpty() && finalBody != null && !finalBody.isEmpty()) {
                    log.warn("续写解析结果为空，finalBody.keys={}，请检查 Coze 是否返回 markdown_content/extended_article 或 final_result.extended_article", finalBody.keySet());
                }
                String fullText = originalContent + "\n\n" + continuation;
                saveResult(taskId, continuation, fullText.length(), platform, null);
                if (handler != null) handler.onComplete();
            }
            @Override
            public void onError(String message) {
                saveResult(taskId, "", 0, platform, message);
                String display = errorLogService.logAndMessage("continue", "Coze续写调用", message != null ? message : "续写失败", null, "taskId=" + taskId);
                if (handler != null) handler.onError(display);
            }
        });
    }

    private void saveResult(Long taskId, String continuation, int totalLength, String platform, String error) {
        ContinuationTask task = new ContinuationTask();
        task.setId(taskId);
        task.setContinuation(continuation);
        task.setTotalLength(totalLength);
        task.setPlatform(platform);
        task.setStatus(error == null ? 1 : 2);
        task.setCompletedAt(LocalDateTime.now());
        continuationMapper.updateResult(task);
    }

    public ContinuationTask getTask(Long id) {
        return continuationMapper.selectById(id);
    }

    public interface StreamHandler {
        void onContent(String text);
        void onComplete();
        void onError(String error);
    }
}
