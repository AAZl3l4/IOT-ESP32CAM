package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * LED亮度设置请求
 */
@Data
public class LedBrightnessRequest {
    
    @NotNull(message = "亮度值不能为空")
    @Min(value = 0, message = "亮度值必须在0-255之间")
    @Max(value = 255, message = "亮度值必须在0-255之间")
    private Integer brightness;
}
