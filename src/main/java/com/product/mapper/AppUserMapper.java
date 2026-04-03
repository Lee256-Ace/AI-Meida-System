package com.product.mapper;

import com.product.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppUserMapper {

    AppUser selectByUsername(@Param("username") String username);

    List<AppUser> selectAll();

    AppUser selectById(@Param("id") Integer id);

    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    int deleteById(@Param("id") Integer id);

    int insert(AppUser user);
}
