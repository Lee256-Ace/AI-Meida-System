package com.product.service;

import com.product.entity.ContinuationTask;
import com.product.mapper.ContinuationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContinuationService {

    private final ContinuationMapper continuationMapper;
    private final QwenWorkflowClient qwenWorkflowClient;
    private final ErrorLogService errorLogService;

    @Transactional
    public Long createTask(String originalContent, String contextHint, String platform) {
        ContinuationTask task = new ContinuationTask();
        task.setOriginalContent(originalContent);
        task.setContextHint(contextHint);
        task.setPlatform(platform);
        task.setModel("qwen");
        task.setStatus(0);
        task.setCreatedAt(LocalDateTime.now());
        continuationMapper.insert(task);
        return task.getId();
    }

    /**
     * 续写：调用通义千问大模型，将原 Coze 的 operations=["extend"] 载荷转换为自然语言提示词。
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

        String systemPrompt = "你是一名资深的中文内容创作者，擅长根据已有文章进行自然流畅的续写。仅输出续写正文，不要输出任何解释、前缀、标题或提示语。";
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请对以下文章进行续写：\n").append(originalContent.trim()).append("\n");
        userPrompt.append("续写长度：").append(extendLength != null && !extendLength.isBlank() ? extendLength.trim() : "中等").append("\n");
        userPrompt.append("续写风格：").append(extendStyle != null && !extendStyle.isBlank() ? extendStyle.trim() : "保持原风格").append("\n");
        if (contextHint != null && !contextHint.isBlank()) {
            userPrompt.append("续写方向提示：").append(contextHint.trim()).append("\n");
        }
        userPrompt.append("请开始输出续写正文。");

        qwenWorkflowClient.generateStream(systemPrompt, userPrompt.toString(), new QwenWorkflowClient.StreamHandler() {
            @Override
            public void onContent(String text) {
                if (handler != null) handler.onContent(text);
            }
            @Override
            public void onComplete(String fullText) {
                String continuation = fullText != null ? fullText.trim() : "";
                String fullArticle = originalContent + "\n\n" + continuation;
                saveResult(taskId, continuation, fullArticle.length(), platform, null);
                if (handler != null) handler.onComplete();
            }
            @Override
            public void onError(String message) {
                saveResult(taskId, "", 0, platform, message);
                String display = errorLogService.logAndMessage("continue", "千问续写调用", message != null ? message : "续写失败", null, "taskId=" + taskId);
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
