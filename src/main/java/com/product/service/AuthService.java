package com.product.service;

import com.product.dto.AppUserRegisterRequest;
import com.product.dto.LoginResponse;
import com.product.entity.Admin;
import com.product.entity.AppUser;
import com.product.mapper.AdminMapper;
import com.product.mapper.AppUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务类 - 简化版本
 */
@Service
public class AuthService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private ErrorLogService errorLogService;

    /**
     * 管理员登录
     */
    public LoginResponse adminLogin(String username, String password) {
        Admin admin = adminMapper.selectByUsername(username);
        if (admin != null) {
            if (!password.equals(admin.getPassword())) {
                errorLogService.logAndMessage("auth", "登录", "密码错误", null, "username=" + (username != null ? username : ""));
                throw new RuntimeException("密码错误");
            }
            if (admin.getStatus() == 0) {
                errorLogService.logAndMessage("auth", "登录", "账号已被禁用", null, "username=" + (username != null ? username : ""));
                throw new RuntimeException("账号已被禁用");
            }

            // 约定：只有 username=admin 的才算真正管理员
            String userType = "admin".equals(admin.getUsername()) ? "admin" : "user";

            return new LoginResponse(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getRealName(),
                    userType,
                    admin.getAvatar(),
                    admin.getPermissions()
            );
        }

        // 管理员表未找到 -> 尝试普通用户表
        AppUser user = appUserMapper.selectByUsername(username);
        if (user == null) {
            errorLogService.logAndMessage("auth", "登录", "用户不存在", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("用户不存在");
        }
        if (!password.equals(user.getPassword())) {
            errorLogService.logAndMessage("auth", "登录", "密码错误", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() == 0) {
            errorLogService.logAndMessage("auth", "登录", "账号已被禁用", null, "username=" + (username != null ? username : ""));
            throw new RuntimeException("账号已被禁用");
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                "user",
                user.getAvatar(),
                null
        );
    }

    /**
     * 注册普通用户（写入 users 表）。公开接口，任意访客或已登录用户均可调用。
     */
    public Integer registerNormalUser(AppUserRegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }

        if (adminMapper.selectByUsername(username) != null
                || appUserMapper.selectByUsername(username) != null) {
            throw new RuntimeException("该用户名已存在");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(request.getPassword());
        String rn = request.getRealName();
        user.setRealName(rn == null || rn.isBlank() ? null : rn.trim());
        String phone = request.getPhone();
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        String email = request.getEmail();
        user.setEmail(email == null || email.isBlank() ? null : email.trim());
        user.setStatus(1);
        appUserMapper.insert(user);
        return user.getId();
    }

}
