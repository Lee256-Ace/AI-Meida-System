package com.product.common;

import com.alibaba.fastjson2.JSON;

/**
 * 统一响应结果类
 */
public class Result<T> {
    
    private Integer code;
    private String message;
    private T data;
    
    public Result() {}
    
    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败");
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }
    
    /**
     * 参数错误
     */
    public static <T> Result<T> paramError(String message) {
        return new Result<>(400, message);
    }
    
    /**
     * 未授权
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "未授权");
    }
    
    /**
     * 禁止访问
     */
    public static <T> Result<T> forbidden() {
        return new Result<>(403, "禁止访问");
    }
    
    /**
     * 资源不存在
     */
    public static <T> Result<T> notFound() {
        return new Result<>(404, "资源不存在");
    }
    
    // Getter and Setter
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
} 