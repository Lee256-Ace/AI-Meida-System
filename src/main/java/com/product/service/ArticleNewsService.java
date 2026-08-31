package com.product.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.product.common.PageResult;
import com.product.entity.ArticleNews;
import com.product.mapper.ArticleNewsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * ArticleNews服务类
 * 
 * @author CodeGenerator
 * @date 2026-03-01 17:15:34
 */
@Service
public class ArticleNewsService {

    @Autowired
    private ArticleNewsMapper articlenewsMapper;

    @Autowired
    private QwenWorkflowClient qwenWorkflowClient;

    /** 热点保留天数，超过该天数的热点将被定时清理 */
    @Value("${news.cleanup.retention-days:3}")
    private int retentionDays;

    /**
     * 根据ID查询
     */
    public ArticleNews getById(String id) {
        return articlenewsMapper.selectById(id);
    }

    /**
     * 分页查询
     */
    public PageResult<ArticleNews> getPage(Integer page, Integer size, String title, String type) {
        Integer offset = (page - 1) * size;
        // 检查是否有搜索条件
        boolean hasSearchCondition = (title != null && !title.trim().isEmpty())
                || (type != null && !type.trim().isEmpty());

        List<ArticleNews> list;
        Integer total;

        if (hasSearchCondition) {
            // 使用带搜索条件的查询
            list = articlenewsMapper.selectByPageWithSearch(offset, size, title, type);
            total = articlenewsMapper.countTotalWithSearch(title, type);
        } else {
            // 使用普通分页查询
            list = articlenewsMapper.selectByPage(offset, size);
            total = articlenewsMapper.countTotal();
        }

        return new PageResult<>(list, total.longValue(), page, size);
    }

    /**
     * 使用千问大模型总结文章内容
     */
    public String summarize(String id) {
        ArticleNews news = articlenewsMapper.selectById(id);
        if (news == null) {
            throw new RuntimeException("文章不存在");
        }
        // 已总结过直接读库，避免重复调用大模型
        if (news.getSummary() != null && !news.getSummary().trim().isEmpty()) {
            return news.getSummary();
        }
        String title = news.getTitle() == null ? "" : news.getTitle();
        String content = news.getContent() == null ? "" : news.getContent();
        String systemPrompt = "你是新闻编辑，用一句话提炼新闻核心（时间、地点、人物、事件），不超过80字，只输出摘要本身。";
        String userPrompt = "标题：" + title + "\n内容：" + content;
        String summary = qwenWorkflowClient.generateSummary(systemPrompt, userPrompt, 200);
        if (summary != null && !summary.trim().isEmpty()) {
            articlenewsMapper.updateSummary(news.getId(), summary.trim());
        }
        return summary;
    }

    /**
     * 查询所有
     */
    public List<ArticleNews> getList() {
        return articlenewsMapper.selectAll();
    }

    /**
     * 新增
     */
    public void add(ArticleNews articlenews) {
        articlenewsMapper.insert(articlenews);
    }

    /**
     * 更新
     */
    public void update(ArticleNews articlenews) {
        articlenewsMapper.update(articlenews);
    }

    /**
     * 删除
     */
    public void delete(String id) {
        articlenewsMapper.deleteById(id);
    }

    /**
     * 根据标题和更新或者保存新浪新闻
     *
     * @param hotNewsEntity
     * @return
     * @Author: ZQCN
     * @Updator: ZQCN
     * @Date 2025/2/18 9:41
     */
    public void updateOrSaveSinaNewsBy(ArticleNews hotNewsEntity){
        // 获取今天的开始时间和结束时间
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<ArticleNews> list = articlenewsMapper.selectList(
                Wrappers.lambdaQuery(ArticleNews.class)
                        .eq(ArticleNews::getSource, hotNewsEntity.getSource())
                        .eq(ArticleNews::getType, hotNewsEntity.getType())
                        .eq(ArticleNews::getTitle, hotNewsEntity.getTitle())
                        .between(ArticleNews::getCreateTime, startOfDay, endOfDay)
                        .last(" limit 1 ")
        );
        LocalDateTime now = LocalDateTime.now();
        if (CollectionUtils.isNotEmpty(list)) {
            ArticleNews exist = list.get(0);
            hotNewsEntity.setId(exist.getId());
            // 保留原创建时间，仅刷新更新时间
            hotNewsEntity.setCreateTime(exist.getCreateTime());
            hotNewsEntity.setUpdateTime(now);
        }
        else {
            hotNewsEntity.setCreateTime(now);
            hotNewsEntity.setUpdateTime(now);
        }

        if (hotNewsEntity.getId() != null){
            articlenewsMapper.update(hotNewsEntity);
        }else {
            articlenewsMapper.insert(hotNewsEntity);
        }
    }

    /**
     * 清理过时热点。
     * 删除规则：新闻日期早于保留阈值；新闻日期为空但创建时间早于阈值；两者均为空的历史遗留数据。
     *
     * @return 删除的记录数
     */
    public int cleanOutdatedNews() {
        LocalDateTime threshold = LocalDate.now().minusDays(retentionDays).atStartOfDay();
        Integer deleted = articlenewsMapper.deleteOutdatedNews(threshold);
        return deleted == null ? 0 : deleted;
    }

}
