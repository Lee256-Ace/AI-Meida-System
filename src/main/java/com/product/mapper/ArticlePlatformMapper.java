package com.product.mapper;

import com.product.entity.ArticlePlatform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章发布的平台Mapper接口
 * 
 * @author CodeGenerator
 * @date 2026-02-25 15:06:28
 */
@Mapper
public interface ArticlePlatformMapper {

    /**
     * 根据ID查询文章发布的平台
     */
    ArticlePlatform selectById(@Param("id") String id);

    /**
     * 查询所有文章发布的平台
     */
    List<ArticlePlatform> selectAll();

    /**
     * 分页查询文章发布的平台
     */
    List<ArticlePlatform> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计总数
     */
    Integer countTotal();

    /**
     * 插入文章发布的平台
     */
    Integer insert(ArticlePlatform articleplatform);

    /**
     * 更新文章发布的平台
     */
    Integer update(ArticlePlatform articleplatform);

    /**
     * 根据ID删除文章发布的平台
     */
    Integer deleteById(@Param("id") String id);

    /**
     * 分页查询（带搜索条件）
     */
    List<ArticlePlatform> selectByPageWithSearch(@Param("offset") Integer offset, 
                                          @Param("limit") Integer limit,
                                          @Param("name") String name);

    /**
     * 统计总数（带搜索条件）
     */
    Integer countTotalWithSearch(@Param("name") String name);

}
