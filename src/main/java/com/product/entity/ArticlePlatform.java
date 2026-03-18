package com.product.entity;

import java.time.LocalDateTime;

/**
 * 文章发布的平台实体类
 * 
 * @author CodeGenerator
 * @date 2026-02-25 15:06:28
 */
public class ArticlePlatform {

    private String id;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建日期
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新日期
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除，0正常1已删除
     */
    private Integer delFlag;

    /**
     * 平台名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    public ArticlePlatform() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
