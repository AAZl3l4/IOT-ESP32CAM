package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 舵机控制请求 (窗户控制)
 */
@Data
public class ServoRequest {
    
    @NotNull(message = "舵机角度不能为空")
    @Min(value = 0, message = "角度最小为0")
    @Max(value = 180, message = "角度最大为180")
    private Integer angle;
}
