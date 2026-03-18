package com.product.entity;

import java.time.LocalDateTime;

/**
 * ArticleNews实体类
 * 
 * @author CodeGenerator
 * @date 2026-03-01 17:15:34
 */
public class ArticleNews {

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
     * 所属部门
     */
    private String sysOrgCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除，0正常1已删除
     */
    private Integer delFlag;

    /**
     * 新闻来源
     */
    private String source;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 新闻内容
     */
    private String content;

    /**
     * 新闻链接
     */
    private String url;

    /**
     * 新闻图片
     */
    private String pic;

    /**
     * 热度指数
     */
    private Integer hot;

    /**
     * 新闻类型
     */
    private String type;

    /**
     * 新闻日期
     */
    private LocalDateTime newsDate;

    /**
     * 媒体
     */
    private String media;

    /**
     * 图片数量
     */
    private Integer picCount;

    /**
     * 图片列表
     */
    private String picList;

    /**
     * 选择多少字数以内的文章发布
     */
    private Integer wordNum;

    public ArticleNews() {}

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

    public String getSysOrgCode() {
        return sysOrgCode;
    }

    public void setSysOrgCode(String sysOrgCode) {
        this.sysOrgCode = sysOrgCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public Integer getHot() {
        return hot;
    }

    public void setHot(Integer hot) {
        this.hot = hot;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getNewsDate() {
        return newsDate;
    }

    public void setNewsDate(LocalDateTime newsDate) {
        this.newsDate = newsDate;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public Integer getPicCount() {
        return picCount;
    }

    public void setPicCount(Integer picCount) {
        this.picCount = picCount;
    }

    public String getPicList() {
        return picList;
    }

    public void setPicList(String picList) {
        this.picList = picList;
    }

    public Integer getWordNum() {
        return wordNum;
    }

    public void setWordNum(Integer wordNum) {
        this.wordNum = wordNum;
    }

}
