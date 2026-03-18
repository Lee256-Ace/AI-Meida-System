package com.product.controller;

import com.product.common.Result;
import com.product.common.PageResult;
import com.product.entity.ArticlePlatform;
import com.product.service.ArticlePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章发布的平台控制器
 * 
 * @author CodeGenerator
 * @date 2026-02-25 15:06:28
 */
@RestController
@RequestMapping("/articlePlatform")
public class ArticlePlatformController {

    @Autowired
    private ArticlePlatformService articleplatformService;

    /**
     * 根据ID查询文章发布的平台
     */
    @GetMapping("/{id}")
    public Result<ArticlePlatform> getById(@PathVariable String id) {
        ArticlePlatform result = articleplatformService.getById(id);
        return Result.success(result);
    }

    /**
     * 分页查询文章发布的平台
     */
    @GetMapping("/page")
    public Result<PageResult<ArticlePlatform>> getPage(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String name) {
        PageResult<ArticlePlatform> result = articleplatformService.getPage(page, size, name);
        return Result.success(result);
    }

    /**
     * 查询所有文章发布的平台
     */
    @GetMapping("/list")
    public Result<List<ArticlePlatform>> getList() {
        List<ArticlePlatform> result = articleplatformService.getList();
        return Result.success(result);
    }

    /**
     * 新增文章发布的平台
     */
    @PostMapping
    public Result<String> add(@RequestBody ArticlePlatform articleplatform) {
        articleplatformService.add(articleplatform);
        return Result.success("新增成功");
    }

    /**
     * 更新文章发布的平台
     */
    @PutMapping
    public Result<String> update(@RequestBody ArticlePlatform articleplatform) {
        articleplatformService.update(articleplatform);
        return Result.success("更新成功");
    }

    /**
     * 删除文章发布的平台
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        articleplatformService.delete(id);
        return Result.success("删除成功");
    }

}
