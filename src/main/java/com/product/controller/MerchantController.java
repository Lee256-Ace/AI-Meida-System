package com.product.controller;

import com.product.common.Result;
import com.product.common.PageResult;
import com.product.entity.Merchant;
import com.product.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家表控制器
 * 
 * @author CodeGenerator
 * @date 2025-09-11 15:24:32
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    /**
     * 根据ID查询商家表
     */
    @GetMapping("/{id}")
    public Result<Merchant> getById(@PathVariable Integer id) {
        Merchant result = merchantService.getById(id);
        return Result.success(result);
    }

    /**
     * 分页查询商家表
     */
    @GetMapping("/page")
    public Result<PageResult<Merchant>> getPage(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String shopName,
                                                     @RequestParam(required = false) String shopAddress,
                                                     @RequestParam(required = false) String contactName,
                                                     @RequestParam(required = false) String email) {
        PageResult<Merchant> result = merchantService.getPage(page, size, username, shopName, shopAddress, contactName, email);
        return Result.success(result);
    }

    /**
     * 查询所有商家表
     */
    @GetMapping("/list")
    public Result<List<Merchant>> getList() {
        List<Merchant> result = merchantService.getList();
        return Result.success(result);
    }

    /**
     * 新增商家表
     */
    @PostMapping
    public Result<String> add(@RequestBody Merchant merchant) {
        merchantService.add(merchant);
        return Result.success("新增成功");
    }

    /**
     * 更新商家表
     */
    @PutMapping
    public Result<String> update(@RequestBody Merchant merchant) {
        merchantService.update(merchant);
        return Result.success("更新成功");
    }

    /**
     * 删除商家表
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        merchantService.delete(id);
        return Result.success("删除成功");
    }

}
