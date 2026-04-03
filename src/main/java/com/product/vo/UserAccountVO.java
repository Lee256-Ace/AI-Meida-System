package com.product.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理列表展示用（admins + users）
 *
 * 注意：出于安全原因，password 不返回明文，而是脱敏字段 passwordMasked。
 */
@Data
public class UserAccountVO {
    /** 记录来源：admin（admins 表）、app_user（users 表，可封禁/删除） */
    private String recordSource;
    private String accountType; // admin / user
    private Integer id;
    private String username;
    private String passwordMasked;
    private Integer status;
    private String shopName; // 保留字段，恒为 null
    private LocalDateTime createdAt;
}

