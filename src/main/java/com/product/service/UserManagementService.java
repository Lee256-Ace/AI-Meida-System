package com.product.service;

import com.product.entity.Admin;
import com.product.entity.AppUser;
import com.product.mapper.AdminMapper;
import com.product.mapper.AppUserMapper;
import com.product.vo.UserAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员用户管理：聚合 admins + users 列表；对 {@code users} 表账号支持封禁/删除。
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AdminMapper adminMapper;
    private final AppUserMapper appUserMapper;

    public List<UserAccountVO> listAllAccounts() {
        List<UserAccountVO> result = new ArrayList<>();

        List<Admin> admins = adminMapper.selectAll();
        for (Admin a : admins) {
            UserAccountVO vo = new UserAccountVO();
            // 仅 username=admin 视为“管理员”，其余 admins 表记录在展示层按普通用户处理
            boolean isRealAdmin = a.getUsername() != null && a.getUsername().trim().equals("admin");
            vo.setAccountType(isRealAdmin ? "admin" : "user");
            vo.setRecordSource("admin");
            vo.setId(a.getId());
            vo.setUsername(a.getUsername());
            vo.setPasswordMasked(maskPassword(a.getPassword()));
            vo.setStatus(a.getStatus());
            vo.setShopName(null);
            vo.setCreatedAt(a.getCreatedTime());
            result.add(vo);
        }

        List<AppUser> users = appUserMapper.selectAll();
        for (AppUser u : users) {
            UserAccountVO vo = new UserAccountVO();
            vo.setAccountType("user");
            vo.setRecordSource("app_user");
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setPasswordMasked(maskPassword(u.getPassword()));
            vo.setStatus(u.getStatus());
            vo.setShopName(null);
            vo.setCreatedAt(u.getCreatedAt());
            result.add(vo);
        }

        // 管理员排最前，其余按创建时间倒序（无创建时间的排在同组末尾）
        result.sort((x, y) -> {
            boolean xAdmin = "admin".equals(x.getAccountType());
            boolean yAdmin = "admin".equals(y.getAccountType());
            if (xAdmin != yAdmin) {
                return xAdmin ? -1 : 1;
            }
            LocalDateTime a = x.getCreatedAt();
            LocalDateTime b = y.getCreatedAt();
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return b.compareTo(a);
        });

        return result;
    }

    /** 封禁/解禁 users 表账号（status：0 禁用，1 正常） */
    public void setAppUserStatus(Integer id, int status) {
        if (id == null) {
            throw new RuntimeException("用户 id 无效");
        }
        if (status != 0 && status != 1) {
            throw new RuntimeException("状态仅支持 0（禁用）或 1（正常）");
        }
        AppUser u = appUserMapper.selectById(id);
        if (u == null) {
            throw new RuntimeException("用户不存在");
        }
        appUserMapper.updateStatus(id, status);
    }

    /** 删除 users 表账号 */
    public void deleteAppUser(Integer id) {
        if (id == null) {
            throw new RuntimeException("用户 id 无效");
        }
        int n = appUserMapper.deleteById(id);
        if (n == 0) {
            throw new RuntimeException("用户不存在或已删除");
        }
    }

    private String maskPassword(String password) {
        if (password == null) return null;
        String pw = password.trim();
        if (pw.isEmpty()) return "";
        if (pw.length() <= 2) {
            return "*".repeat(pw.length());
        }
        // 脱敏：只展示末尾 2 位
        int maskLen = pw.length() - 2;
        return "*".repeat(maskLen) + pw.substring(pw.length() - 2);
    }
}
