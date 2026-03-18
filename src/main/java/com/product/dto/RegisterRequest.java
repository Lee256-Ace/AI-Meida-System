package com.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求DTO - 商家注册
 */
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
    
    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名长度不能超过50个字符")
    private String nickname;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称长度不能超过100个字符")
    private String shopName;
    
    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 200, message = "店铺地址长度不能超过200个字符")
    private String shopAddress;
    
    public RegisterRequest() {}
    
    // Getters and Setters
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
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getShopName() {
        return shopName;
    }
    
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    public String getShopAddress() {
        return shopAddress;
    }
    
    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }
}
