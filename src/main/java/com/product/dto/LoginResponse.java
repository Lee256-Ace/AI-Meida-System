package com.product.dto;

/**
 * 登录响应DTO
 */
public class LoginResponse {
    
    private Integer id;
    private String username;
    private String realName;
    private String userType;
    private String avatar;
    private String permissions;
    private Integer certificationStatus;
    private Integer points;
    private java.math.BigDecimal balance;
    private String nickname;
    private String phone;
    private String email;

    
    public LoginResponse() {}
    
    public LoginResponse(Integer id, String username, String realName, String userType, String avatar, String permissions) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.userType = userType;
        this.avatar = avatar;
        this.permissions = permissions;
    }
    
    public LoginResponse(Integer id, String username, String realName, String userType, String avatar, String permissions, Integer certificationStatus) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.userType = userType;
        this.avatar = avatar;
        this.permissions = permissions;
        this.certificationStatus = certificationStatus;
    }
    
    // Getter and Setter
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public Integer getCertificationStatus() {
        return certificationStatus;
    }
    
    public void setCertificationStatus(Integer certificationStatus) {
        this.certificationStatus = certificationStatus;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public Integer getPoints() {
        return points;
    }

    public void setBalance(java.math.BigDecimal balance) {
        this.balance = balance;
    }
    
    public java.math.BigDecimal getBalance() {
        return balance;
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

}