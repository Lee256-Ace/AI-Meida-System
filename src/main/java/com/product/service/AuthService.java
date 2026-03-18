package com.product.service;

import com.product.dto.LoginResponse;
import com.product.dto.RegisterRequest;
import com.product.entity.Admin;
import com.product.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务类 - 简化版本
 */
@Service
public class AuthService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ErrorLogService errorLogService;

    /**
     * 管理员登录
     */
    public LoginResponse adminLogin(String username, String password) {
        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            errorLogService.logAndMessage("auth", "登录", "管理员不存在", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("管理员不存在");
        }
        if (!password.equals(admin.getPassword())) {
            errorLogService.logAndMessage("auth", "登录", "密码错误", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("密码错误");
        }
        if (admin.getStatus() == 0) {
            errorLogService.logAndMessage("auth", "登录", "账号已被禁用", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("账号已被禁用");
        }
        
        return new LoginResponse(
            admin.getId(),
            admin.getUsername(),
            admin.getRealName(),
            "admin",
            admin.getAvatar(),
            admin.getPermissions()
        );
    }

}
