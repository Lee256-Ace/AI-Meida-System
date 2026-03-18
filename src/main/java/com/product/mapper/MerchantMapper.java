package com.product.mapper;

import com.product.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家表Mapper接口
 * 
 * @author CodeGenerator
 * @date 2025-09-11 15:24:32
 */
@Mapper
public interface MerchantMapper {

    /**
     * 根据ID查询商家表
     */
    Merchant selectById(@Param("id") Integer id);

    /**
     * 查询所有商家表
     */
    List<Merchant> selectAll();

    /**
     * 分页查询商家表
     */
    List<Merchant> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计总数
     */
    Integer countTotal();

    /**
     * 插入商家表
     */
    Integer insert(Merchant merchant);

    /**
     * 更新商家表
     */
    Integer update(Merchant merchant);

    /**
     * 根据ID删除商家表
     */
    Integer deleteById(@Param("id") Integer id);

    /**
     * 根据用户名查询
     */
    Merchant selectByUsername(@Param("username") String username);

    /**
     * 分页查询（带搜索条件）
     */
    List<Merchant> selectByPageWithSearch(@Param("offset") Integer offset, 
                                          @Param("limit") Integer limit,
                                          @Param("username") String username,
                                          @Param("shopName") String shopName,
                                          @Param("shopAddress") String shopAddress,
                                          @Param("contactName") String contactName,
                                          @Param("email") String email);

    /**
     * 统计总数（带搜索条件）
     */
    Integer countTotalWithSearch(@Param("username") String username,
                                 @Param("shopName") String shopName,
                                 @Param("shopAddress") String shopAddress,
                                 @Param("contactName") String contactName,
                                 @Param("email") String email);

}
