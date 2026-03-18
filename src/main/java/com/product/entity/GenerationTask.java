package com.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GenerationTask {
    private Long id;
    private String prompt;
    private String reasoning;
    private String content;
    /** 抖音文案 */
    private String douyinCopy;
    /** 小红书文案 */
    private String xiaohongshuCopy;
    /** 视频号文案 */
    private String videoAccountCopy;
    /** 今日头条文案 */
    private String toutiaoCopy;
    private String model;
    private Integer status;
    private Integer wordCount;
    private String style;
    private String theme;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}