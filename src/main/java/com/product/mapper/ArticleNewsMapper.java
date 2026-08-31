package com.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.product.entity.ArticleNews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ArticleNewsMapper接口
 * 
 * @author CodeGenerator
 * @date 2026-03-01 17:15:34
 */
@Mapper
public interface ArticleNewsMapper extends BaseMapper<ArticleNews> {

    /**
     * 根据ID查询
     */
    ArticleNews selectById(@Param("id") String id);

    /**
     * 查询所有
     */
    List<ArticleNews> selectAll();

    /**
     * 分页查询
     */
    List<ArticleNews> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计总数
     */
    Integer countTotal();

//    /**
//     * 插入
//     */
//    Integer insert(ArticleNews articlenews);

    /**
     * 更新
     */
    Integer update(ArticleNews articlenews);

    /**
     * 根据ID删除
     */
    Integer deleteById(@Param("id") String id);

    /**
     * 删除过时热点
     */
    Integer deleteOutdatedNews(@Param("threshold") LocalDateTime threshold);

    /**
     * 更新新闻摘要（缓存）
     */
    Integer updateSummary(@Param("id") String id, @Param("summary") String summary);

    /**
     * 分页查询（带搜索条件）
     */
    List<ArticleNews> selectByPageWithSearch(@Param("offset") Integer offset, 
                                          @Param("limit") Integer limit,
                                          @Param("title") String title,
                                          @Param("type") String type);

    /**
     * 统计总数（带搜索条件）
     */
    Integer countTotalWithSearch(@Param("title") String title,
                                 @Param("type") String type);

}
