package com.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PolishTask {
    private Long id;
    private String originalContent;
    private String polishType;
    private String targetStyle;
    /** 所属平台：douyin/xiaohongshu/video_account/toutiao */
    private String platform;
    private String resultContent;
    private String changes;
    private String model;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}