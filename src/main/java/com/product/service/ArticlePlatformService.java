package com.product.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.product.common.PageResult;
import com.product.entity.ArticlePlatform;
import com.product.mapper.ArticlePlatformMapper;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章发布的平台服务类
 * 
 * @author CodeGenerator
 * @date 2026-02-25 15:06:28
 */
@Service
public class ArticlePlatformService {

    @Autowired
    private ArticlePlatformMapper articleplatformMapper;

    /**
     * 根据ID查询文章发布的平台
     */
    public ArticlePlatform getById(String id) {
        return articleplatformMapper.selectById(id);
    }

    /**
     * 分页查询文章发布的平台
     */
    public PageResult<ArticlePlatform> getPage(Integer page, Integer size, String name) {
        Integer offset = (page - 1) * size;
        // 检查是否有搜索条件
        boolean hasSearchCondition = (name != null && !name.trim().isEmpty());

        List<ArticlePlatform> list;
        Integer total;

        if (hasSearchCondition) {
            // 使用带搜索条件的查询
            list = articleplatformMapper.selectByPageWithSearch(offset, size, name);
            total = articleplatformMapper.countTotalWithSearch(name);
        } else {
            // 使用普通分页查询
            list = articleplatformMapper.selectByPage(offset, size);
            total = articleplatformMapper.countTotal();
        }

        return new PageResult<>(list, total.longValue(), page, size);
    }

    /**
     * 查询所有文章发布的平台
     */
    public List<ArticlePlatform> getList() {
        return articleplatformMapper.selectAll();
    }

    /**
     * 新增文章发布的平台
     */
    public void add(ArticlePlatform articleplatform) {
        articleplatform.setId(IdWorker.getIdStr());
        articleplatformMapper.insert(articleplatform);
    }

    /**
     * 更新文章发布的平台
     */
    public void update(ArticlePlatform articleplatform) {
        articleplatformMapper.update(articleplatform);
    }

    /**
     * 删除文章发布的平台
     */
    public void delete(String id) {
        articleplatformMapper.deleteById(id);
    }

}
