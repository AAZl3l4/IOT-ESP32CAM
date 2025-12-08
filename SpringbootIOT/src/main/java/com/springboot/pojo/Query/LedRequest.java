package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * LED控制请求
 */
@Data
public class LedRequest {
    
    @NotNull(message = "LED状态不能为空")
    @Min(value = 0, message = "LED状态必须为0或1")
    @Max(value = 1, message = "LED状态必须为0或1")
    private Integer value;
}
