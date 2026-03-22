package com.product.controller;

import com.product.common.Result;
import com.product.dto.MarkdownCopySaveRequest;
import com.product.service.MarkdownCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/markdown-copy")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarkdownCopyController {

    private final MarkdownCopyService markdownCopyService;

    @PostMapping("/save")
    public Result<Map<String, Long>> save(@RequestBody MarkdownCopySaveRequest req) {
        if (req == null || !StringUtils.hasText(req.getContent())) {
            return Result.paramError("正文内容不能为空");
        }
        Long id = markdownCopyService.save(req);
        return Result.success(Map.of("id", id));
    }
}
