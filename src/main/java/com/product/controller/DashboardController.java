package com.product.controller;

import com.product.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin
public class DashboardController {
    
    /**
     * 获取系统基础信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("systemName", "基础管理系统");
        info.put("version", "v1.0.0");
        info.put("status", "运行正常");
        info.put("description", "这是一个基础的管理系统框架");
        return Result.success(info);
    }
}