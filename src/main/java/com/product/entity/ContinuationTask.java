package com.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContinuationTask {
    private Long id;
    private String originalContent;
    private String contextHint;
    /** 所属平台：douyin/xiaohongshu/video_account/toutiao */
    private String platform;
    private String continuation;
    private Integer totalLength;
    private String model;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}