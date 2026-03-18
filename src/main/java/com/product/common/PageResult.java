package com.product.common;

import java.util.List;
import lombok.Data;
@Data

/**
 * 分页结果类
 */
public class PageResult<T> {
    
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrev;
    
    public PageResult() {}
    
    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }
    
    public PageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }
    
    public List<T> getList() {
        return list;
    }
    
    public void setList(List<T> list) {
        this.list = list;
    }
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
}