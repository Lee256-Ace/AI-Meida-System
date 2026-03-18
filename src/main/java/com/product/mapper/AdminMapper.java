package com.product.mapper;

import com.product.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员Mapper接口
 */
@Mapper
public interface AdminMapper {
    
    /**
     * 根据用户名查询管理员
     */
    Admin selectByUsername(@Param("username") String username);
    
    /**
     * 根据ID查询管理员
     */
    Admin selectById(@Param("id") Integer id);
    
    /**
     * 查询所有管理员
     */
    List<Admin> selectAll();
    
    /**
     * 分页查询管理员
     */
    List<Admin> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 统计管理员总数
     */
    Integer countTotal();
    
    /**
     * 插入管理员
     */
    Integer insert(Admin admin);
    
    /**
     * 更新管理员
     */
    Integer update(Admin admin);
    
    /**
     * 删除管理员
     */
    Integer deleteById(@Param("id") Integer id);
    
    /**
     * 更新管理员状态
     */
    Integer updateStatus(@Param("id") Integer id, @Param("status") Integer status);
} 