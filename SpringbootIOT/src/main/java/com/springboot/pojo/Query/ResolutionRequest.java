package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 分辨率设置请求
 */
@Data
public class ResolutionRequest {
    
    @NotNull(message = "分辨率值不能为空")
    @Min(value = 0, message = "分辨率值必须大于等于0")
    @Max(value = 20, message = "分辨率值必须小于等于20")
    private Integer framesize;
}
