package com.product.controller;

import com.product.common.PageResult;
import com.product.entity.ErrorLog;
import com.product.service.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/error-log")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ErrorLogController {

    private final ErrorLogService errorLogService;

    /**
     * 分页查询错误日志列表，便于排查出错的模块与阶段。
     * @param module 可选，按模块筛选：generate / polish / continue / coze / auth
     * @param page 页码，从 1 开始
     * @param size 每页条数
     */
    @GetMapping("/list")
    public PageResult<ErrorLog> list(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return errorLogService.list(module, page, size);
    }

    /**
     * 获取单条错误日志详情（含完整 error_detail 堆栈）
     */
    @GetMapping("/{id}")
    public ErrorLog getById(@PathVariable Long id) {
        return errorLogService.getById(id);
    }

    /**
     * 删除单条错误日志
     */
    @DeleteMapping("/{id}")
    public int deleteById(@PathVariable Long id) {
        return errorLogService.deleteById(id);
    }
}
