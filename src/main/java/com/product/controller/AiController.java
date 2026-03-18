package com.product.controller;

import com.product.entity.*;
import com.product.service.ContinuationService;
import com.product.service.GenerationService;
import com.product.service.PolishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final GenerationService generationService;
    private final PolishService polishService;
    private final ContinuationService continuationService;

    // ========== 文案生成 ==========

    @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "modern") String style,
            @RequestParam(defaultValue = "light") String theme,
            @RequestParam(required = false) String platforms,
            @RequestParam(required = false) String targetAudience) {

        SseEmitter emitter = new SseEmitter(300000L);
        Long taskId = generationService.createTask(prompt, style, theme);
        sendEvent(emitter, "taskId", String.valueOf(taskId));

        List<String> platformList = null;
        if (platforms != null && !platforms.isBlank()) {
            platformList = Arrays.stream(platforms.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        }

        generationService.processTask(taskId, prompt, platformList, targetAudience, new GenerationService.StreamHandler() {
            @Override
            public void onReasoning(String text) {
                sendEvent(emitter, "reasoning", text);
            }
            @Override
            public void onContent(String text) {
                sendEvent(emitter, "content", text);
            }
            @Override
            public void onComplete() {
                sendEvent(emitter, "complete", "done");
                emitter.complete();
            }
            @Override
            public void onError(String error) {
                sendEvent(emitter, "error", error != null ? error : "生成失败");
                emitter.complete();
            }
        });

        return emitter;
    }

    @GetMapping("/generate/task/{id}")
    public GenerationTask getGenerationTask(@PathVariable Long id) {
        return generationService.getTask(id);
    }

    // ========== 文章美化 ==========

    @PostMapping(value = "/polish/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter polishStream(@RequestBody Map<String, String> body) {
        String originalContent = body.get("content");
        String polishType = body.get("type");
        // 官方 target_audience：大众/专业人士/学生/企业，默认大众；兼容旧前端 style
        String targetAudience = body.get("targetAudience");
        if (targetAudience == null || targetAudience.isBlank()) targetAudience = body.get("style");
        String platform = body.get("platform"); // douyin/xiaohongshu/video_account/toutiao，可选

        SseEmitter emitter = new SseEmitter(300000L);
        Long taskId = polishService.createTask(originalContent, polishType, targetAudience, platform);
        sendEvent(emitter, "taskId", String.valueOf(taskId));

        polishService.processTask(taskId, originalContent, polishType, targetAudience, platform, new PolishService.StreamHandler() {
            @Override
            public void onContent(String text) {
                sendEvent(emitter, "content", text);
            }
            @Override
            public void onComplete() {
                sendEvent(emitter, "complete", "done");
                emitter.complete();
            }
            @Override
            public void onError(String error) {
                sendEvent(emitter, "error", error != null ? error : "美化失败");
                emitter.complete();
            }
        });

        return emitter;
    }

    @GetMapping("/polish/task/{id}")
    public PolishTask getPolishTask(@PathVariable Long id) {
        return polishService.getTask(id);
    }

    // ========== 文章续写 ==========

    @PostMapping(value = "/continue/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueStream(@RequestBody Map<String, String> body) {
        String originalContent = body.get("content");
        String contextHint = body.get("hint");
        String platform = body.get("platform");
        String extendLength = body.get("extendLength");
        String extendStyle = body.get("extendStyle");

        SseEmitter emitter = new SseEmitter(300000L);
        Long taskId = continuationService.createTask(originalContent, contextHint, platform);
        sendEvent(emitter, "taskId", String.valueOf(taskId));

        continuationService.processTask(taskId, originalContent, contextHint, platform, extendLength, extendStyle, new ContinuationService.StreamHandler() {
            @Override
            public void onContent(String text) {
                sendEvent(emitter, "content", text);
            }
            @Override
            public void onComplete() {
                sendEvent(emitter, "complete", "done");
                emitter.complete();
            }
            @Override
            public void onError(String error) {
                sendEvent(emitter, "error", error != null ? error : "续写失败");
                emitter.complete();
            }
        });

        return emitter;
    }

    @GetMapping("/continue/task/{id}")
    public ContinuationTask getContinuationTask(@PathVariable Long id) {
        return continuationService.getTask(id);
    }

    // ========== 工具方法 ==========

    private void sendEvent(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data != null ? data : ""));
        } catch (IOException e) {
            log.error("发送SSE失败", e);
        }
    }
}