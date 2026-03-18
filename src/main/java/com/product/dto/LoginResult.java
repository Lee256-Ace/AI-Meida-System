package com.product.dto;

import lombok.Data;

@Data
public class LoginResult {
    private String token;
    private LoginResponse userInfo;

    public LoginResult(String token, LoginResponse userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}