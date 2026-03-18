package com.product.param;

import lombok.Data;

@Data
public class HistoryQueryParam {
    private String type;           // 类型筛选：all/generate/polish/continue
    private String platform;       // 平台筛选：douyin/xiaohongshu/video_account/toutiao
    private String keyword;        // 关键词搜索
    private String status;         // 状态筛选：all/processing/completed/failed
    private String sortBy;         // 排序：time/wordCount
    private String sortOrder;      // 顺序：desc/asc
    private Integer page = 1;      // 页码
    private Integer size = 10;     // 每页大小
    private String startDate;      // 开始日期
    private String endDate;        // 结束日期
}