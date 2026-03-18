package com.product.vo;

import lombok.Data;

@Data
public class StatisticsVO {
    private Long totalCount;      // 总数
    private Long generateCount;   // 生成数量
    private Long polishCount;     // 美化数量
    private Long continueCount;   // 续写数量
    private Long todayCount;      // 今日新增
    private Long totalWords;      // 总字数
}