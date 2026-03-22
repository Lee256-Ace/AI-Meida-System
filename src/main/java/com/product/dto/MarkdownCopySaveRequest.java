package com.product.dto;

import lombok.Data;

/**
 * 保存 Markdown 文案：id 为空则新增，非空且存在则更新。
 */
@Data
public class MarkdownCopySaveRequest {
    private Long id;
    private String title;
    private String content;
}
