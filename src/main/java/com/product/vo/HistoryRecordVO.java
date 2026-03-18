package com.product.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HistoryRecordVO {
    private Long id;
    private String type;           // generate / polish / continue
    private String typeName;       // 文案生成 / 文章美化 / 文章续写
    private String title;          // 标题/提示词摘要
    private String preview;        // 内容预览
    private String status;         // processing / completed / failed
    private String statusName;     // 处理中 / 已完成 / 失败
    private String platform;       // douyin/xiaohongshu/video_account/toutiao，按平台分类
    private String platformName;   // 抖音/小红书/视频号/今日头条
    /** 文案生成任务含有的平台列表（有对应 copy 的平台），用于按平台筛选与展示 */
    private List<String> platforms;
    private LocalDateTime createdAt;
    private String timeAgo;        // 相对时间，如"3分钟前"
    private Integer wordCount;     // 字数
}