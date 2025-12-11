package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 摄像头参数设置请求
 * 支持OV3360传感器参数
 */
@Data
public class CameraParamRequest {
    
    @NotBlank(message = "参数名不能为空")
    @Pattern(regexp = "^(brightness|contrast|saturation|quality|framesize|special_effect|" +
            "awb|awb_gain|wb_mode|" +           // 白平衡相关
            "aec|aec2|ae_level|aec_value|" +    // 曝光控制
            "agc|agc_gain|gainceiling|" +       // 增益控制
            "bpc|wpc|raw_gma|lenc|" +           // 图像校正
            "hmirror|vflip|" +                  // 翻转
            "dcw|colorbar|sharpness|denoise)$", // 其他
             message = "参数名不合法")
    private String name;
    
    @NotNull(message = "参数值不能为空")
    @Min(value = -10, message = "参数值不能小于-10")
    @Max(value = 100, message = "参数值不能大于100")
    private Integer value;
}
