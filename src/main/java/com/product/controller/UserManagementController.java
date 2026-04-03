package com.product.controller;

import com.product.common.Result;
import com.product.service.UserManagementService;
import com.product.util.JwtUtil;
import com.product.vo.UserAccountVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final JwtUtil jwtUtil;

    private void requireAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("请先登录");
        }
        Claims claims = jwtUtil.parseToken(authorizationHeader.substring(7).trim());
        if (claims == null || !"admin".equals(claims.get("userType"))) {
            throw new RuntimeException("仅管理员可操作");
        }
    }

    @GetMapping("/list")
    public Result<List<UserAccountVO>> listAll(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            requireAdmin(authorization);
            return Result.success(userManagementService.listAllAccounts());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 封禁/解禁 users 表普通用户。status：0 禁用，1 正常。
     */
    @PutMapping("/app-user/{id}/status")
    public Result<String> setAppUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Integer id,
            @RequestParam int status) {
        try {
            requireAdmin(authorization);
            if (status != 0 && status != 1) {
                return Result.error("status 只能为 0 或 1");
            }
            userManagementService.setAppUserStatus(id, status);
            return Result.success("操作成功", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除 users 表普通用户（不可逆）。
     */
    @DeleteMapping("/app-user/{id}")
    public Result<String> deleteAppUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Integer id) {
        try {
            requireAdmin(authorization);
            userManagementService.deleteAppUser(id);
            return Result.success("已删除", null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
