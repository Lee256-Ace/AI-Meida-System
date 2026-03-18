package com.product.service;

import com.product.common.PageResult;
import com.product.entity.Merchant;
import com.product.mapper.MerchantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商家表服务类
 * 
 * @author CodeGenerator
 * @date 2025-09-11 15:24:32
 */
@Service
public class MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;

    /**
     * 根据ID查询商家表
     */
    public Merchant getById(Integer id) {
        return merchantMapper.selectById(id);
    }

    /**
     * 分页查询商家表
     */
    public PageResult<Merchant> getPage(Integer page, Integer size, String username, String shopName, String shopAddress, String contactName, String email) {
        Integer offset = (page - 1) * size;
        // 检查是否有搜索条件
        boolean hasSearchCondition = (username != null && !username.trim().isEmpty()) || (shopName != null && !shopName.trim().isEmpty()) || (shopAddress != null && !shopAddress.trim().isEmpty()) || (contactName != null && !contactName.trim().isEmpty()) || (email != null && !email.trim().isEmpty());

        List<Merchant> list;
        Integer total;

        if (hasSearchCondition) {
            // 使用带搜索条件的查询
            list = merchantMapper.selectByPageWithSearch(offset, size, username, shopName, shopAddress, contactName, email);
            total = merchantMapper.countTotalWithSearch(username, shopName, shopAddress, contactName, email);
        } else {
            // 使用普通分页查询
            list = merchantMapper.selectByPage(offset, size);
            total = merchantMapper.countTotal();
        }

        return new PageResult<>(list, total.longValue(), page, size);
    }

    /**
     * 查询所有商家表
     */
    public List<Merchant> getList() {
        return merchantMapper.selectAll();
    }

    /**
     * 新增商家表
     */
    public void add(Merchant merchant) {
        merchantMapper.insert(merchant);
    }

    /**
     * 更新商家表
     */
    public void update(Merchant merchant) {
        merchantMapper.update(merchant);
    }

    /**
     * 删除商家表
     */
    public void delete(Integer id) {
        merchantMapper.deleteById(id);
    }

}
