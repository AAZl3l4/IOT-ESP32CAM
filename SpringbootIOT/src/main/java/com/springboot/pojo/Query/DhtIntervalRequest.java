package com.springboot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DHT读取间隔设置请求
 */
@Data
public class DhtIntervalRequest {
    
    @NotNull(message = "间隔不能为空")
    @Min(value = 1000, message = "间隔最小1000毫秒(1秒)")
    @Max(value = 60000, message = "间隔最大60000毫秒(60秒)")
    private Integer interval;
}
