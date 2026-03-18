package com.product.controller;

import com.product.common.Result;
import com.product.dto.LoginRequest;
import com.product.dto.LoginResponse;
import com.product.dto.LoginResult;
import com.product.dto.RegisterRequest;
import com.product.service.AuthService;
import com.product.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;  // 注入JWT工具

    /**
     * 通用登录接口 - 支持管理员和商家登录
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse userInfo = authService.adminLogin(request.getUsername(), request.getPassword());

            // 将 Integer 转换为 Long
            String token = jwtUtil.generateToken(
                    Long.valueOf(userInfo.getId()),
                    userInfo.getUsername(),
                    userInfo.getUserType()
            );

            LoginResult loginResult = new LoginResult(token, userInfo);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员登录（可保留，但前端可能不再使用）
     */
    @PostMapping("/admin/login")
    public Result<LoginResult> adminLogin(@RequestBody LoginRequest request) {
        try {
            // 注意：adminLogin 需要用户名和密码两个参数
            LoginResponse userInfo = authService.adminLogin(request.getUsername(), request.getPassword());

            // 将 Integer 类型的 id 转换为 Long
            String token = jwtUtil.generateToken(
                    Long.valueOf(userInfo.getId()),
                    userInfo.getUsername(),
                    userInfo.getUserType()   // 确保 LoginResponse 中有 getUserType() 方法
            );

            LoginResult loginResult = new LoginResult(token, userInfo);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 商家注册（保持注释或按需启用）
    // @PostMapping("/merchant/register")
    // public Result<String> merchantRegister(@RequestBody RegisterRequest request) {
    //     try {
    //         authService.merchantRegister(request);
    //         return Result.success("注册成功，请等待管理员审核");
    //     } catch (RuntimeException e) {
    //         return Result.error(e.getMessage());
    //     }
    // }

    /**
     * 获取用户信息（需认证，暂时保留原样）
     */
    @GetMapping("/userInfo")
    public Result<LoginResponse> getUserInfo(@RequestParam String userType, @RequestParam Integer userId) {
        try {
            LoginResponse response = new LoginResponse();
            response.setId(userId);
            response.setUserType(userType);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 用户登出（前端清理token即可，后端可忽略）
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("登出成功");
    }
}