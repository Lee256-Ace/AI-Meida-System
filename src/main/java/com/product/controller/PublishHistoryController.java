package com.product.controller;

import com.product.common.PageResult;
import com.product.common.Result;
import com.product.entity.PublishHistory;
import com.product.service.PublishHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/publishHistory")
@CrossOrigin(origins = "*")
public class PublishHistoryController {

    @Autowired
    private PublishHistoryService publishHistoryService;

    @GetMapping("/{id}")
    public Result<PublishHistory> getById(@PathVariable Long id) {
        return Result.success(publishHistoryService.getById(id));
    }

    @GetMapping("/page")
    public Result<PageResult<PublishHistory>> getPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String platformName,
            @RequestParam(required = false) String title) {
        return Result.success(publishHistoryService.getPage(page, size, platformName, title));
    }

    @PostMapping
    public Result<String> add(@RequestBody PublishHistory row) {
        publishHistoryService.add(row);
        return Result.success("新增成功");
    }

    @PutMapping
    public Result<String> update(@RequestBody PublishHistory row) {
        publishHistoryService.update(row);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        publishHistoryService.delete(id);
        return Result.success("删除成功");
    }
}
