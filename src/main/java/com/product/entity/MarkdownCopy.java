package com.product.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Markdown 编辑器保存至 MySQL 的文案记录（表 ai_markdown_copy）。
 */
@Data
public class MarkdownCopy {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
