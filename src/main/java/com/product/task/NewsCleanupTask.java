package com.product.task;

import com.product.service.ArticleNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热点数据定时清理任务。
 * 定期删除过时的新闻热点，避免数据库堆积大量过期信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCleanupTask {

    private final ArticleNewsService articleNewsService;

    /**
     * 定时清理过时热点，执行时间由配置 news.cleanup.cron 控制（默认每天凌晨 3 点）。
     */
    @Scheduled(cron = "${news.cleanup.cron:0 0 3 * * ?}")
    public void cleanupOutdatedNews() {
        try {
            int deleted = articleNewsService.cleanOutdatedNews();
            log.info("热点定时清理完成，共删除 {} 条过时热点", deleted);
        } catch (Exception e) {
            log.error("热点定时清理失败", e);
        }
    }
}
