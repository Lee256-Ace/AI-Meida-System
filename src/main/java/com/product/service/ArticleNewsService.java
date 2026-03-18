package com.product.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.product.common.PageResult;
import com.product.entity.ArticleNews;
import com.product.mapper.ArticleNewsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
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

    /**
     * 根据ID查询
     */
    public ArticleNews getById(String id) {
        return articlenewsMapper.selectById(id);
    }

    /**
     * 分页查询
     */
    public PageResult<ArticleNews> getPage(Integer page, Integer size, String title) {
        Integer offset = (page - 1) * size;
        // 检查是否有搜索条件
        boolean hasSearchCondition = (title != null && !title.trim().isEmpty());

        List<ArticleNews> list;
        Integer total;

        if (hasSearchCondition) {
            // 使用带搜索条件的查询
            list = articlenewsMapper.selectByPageWithSearch(offset, size, title);
            total = articlenewsMapper.countTotalWithSearch(title);
        } else {
            // 使用普通分页查询
            list = articlenewsMapper.selectByPage(offset, size);
            total = articlenewsMapper.countTotal();
        }

        return new PageResult<>(list, total.longValue(), page, size);
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
        Date date = new Date();
        if (CollectionUtils.isNotEmpty(list)) {
            hotNewsEntity.setId(list.get(0).getId());
//            hotNewsEntity.setUpdateTime(date);
        }
        else {
//            hotNewsEntity.setCreateTime(date);
//            hotNewsEntity.setUpdateTime(date);
        }

        if (hotNewsEntity.getId() != null){
            articlenewsMapper.update(hotNewsEntity);
        }else {
            articlenewsMapper.insert(hotNewsEntity);
        }
    }

}
