package com.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "用户类型不能为空")
    private String userType; // admin, merchant, user
    
    public LoginRequest() {}
    
    public LoginRequest(String username, String password, String userType) {
        this.username = username;
        this.password = password;
        this.userType = userType;
    }
    
    // Getter and Setter
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
} 