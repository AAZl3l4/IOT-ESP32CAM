package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 摄像头参数设置请求
 */
@Data
public class CameraParamRequest {
    
    @NotBlank(message = "参数名不能为空")
    @Pattern(regexp = "^(brightness|contrast|saturation|quality|framesize|special_effect|wb_mode|ae_level)$",
             message = "参数名不合法")
    private String name;
    
    @NotNull(message = "参数值不能为空")
    @Min(value = -10, message = "参数值不能小于-10")
    @Max(value = 100, message = "参数值不能大于100")
    private Integer value;
}
