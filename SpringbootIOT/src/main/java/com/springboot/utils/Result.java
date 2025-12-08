package com.springboot.utils;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应结果类
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {
    
    /**
     * 状态码: 0=成功, 非0=失败
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = Instant.now().toEpochMilli();
    }
    
    public Result(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(0, "操作成功", null);
    }
    
    /**
     * 成功（有数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "操作成功", data);
    }
    
    /**
     * 成功（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(0, message, data);
    }
    
    /**
     * 失败（默认错误码500）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    /**
     * 失败（自定义错误码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
