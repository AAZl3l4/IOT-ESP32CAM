package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * WiFi配置请求
 */
@Data
public class WiFiConfigRequest {
    
    @NotBlank(message = "SSID不能为空")
    @Size(min = 1, max = 32, message = "SSID长度必须在1-32字符之间")
    private String ssid;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 64, message = "密码长度必须在1-64字符之间")
    private String password;
}
