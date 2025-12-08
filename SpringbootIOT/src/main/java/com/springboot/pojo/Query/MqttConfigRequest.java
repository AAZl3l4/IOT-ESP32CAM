package com.springboot.pojo.Query;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * MQTT配置请求
 */
@Data
public class MqttConfigRequest {
    
    @NotBlank(message = "MQTT服务器地址不能为空")
    @Size(max = 256, message = "服务器地址长度不能超过256字符")
    private String server;
    
    @Min(value = 1, message = "端口号必须在1-65535之间")
    @Max(value = 65535, message = "端口号必须在1-65535之间")
    private Integer port = 1883;
    
    @Size(max = 64, message = "客户端ID长度不能超过64字符")
    private String mqttClientId;
}
