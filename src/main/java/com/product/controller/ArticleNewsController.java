package com.product.controller;

import com.product.common.Result;
import com.product.common.PageResult;
import com.product.entity.ArticleNews;
import com.product.service.ArticleNewsService;
import com.product.spider.SinaHotNewsSpider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ArticleNews控制器
 * 
 * @author CodeGenerator
 * @date 2026-03-01 17:15:34
 */
@RestController
@RequestMapping("/articleNews")
public class ArticleNewsController {

    @Autowired
    private ArticleNewsService articlenewsService;
    @Autowired
    private SinaHotNewsSpider hotNewsSpider;

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<ArticleNews> getById(@PathVariable String id) {
        ArticleNews result = articlenewsService.getById(id);
        return Result.success(result);
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Result<PageResult<ArticleNews>> getPage(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String title) {
        PageResult<ArticleNews> result = articlenewsService.getPage(page, size, title);
        return Result.success(result);
    }

    /**
     * 查询所有
     */
    @GetMapping("/list")
    public Result<List<ArticleNews>> getList() {
        List<ArticleNews> result = articlenewsService.getList();
        return Result.success(result);
    }

    /**
     * 抓取数据
     */
    @PostMapping
    public Result<String> add(@RequestBody ArticleNews articlenews) {
        // 抓取数据
        hotNewsSpider.sinaProcess();
        return Result.success("数据获取成功");
    }

    /**
     * 更新
     */
    @PutMapping
    public Result<String> update(@RequestBody ArticleNews articlenews) {
        articlenewsService.update(articlenews);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        articlenewsService.delete(id);
        return Result.success("删除成功");
    }

}
