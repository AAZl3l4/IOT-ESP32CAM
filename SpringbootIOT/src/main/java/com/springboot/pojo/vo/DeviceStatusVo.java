package com.springboot.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备状态推送VO（SSE推送用）
 * 包含设备的完整状态和配置信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusVo {
    
    // === 基础信息 ===
    private String clientId;
    private Long uptime;         // 运行时间(秒)
    private Integer freeHeap;    // 空闲内存
    private Integer rssi;        // WiFi信号强度
    
    // === LED状态 ===
    private Boolean ledStatus;
    private Integer ledBrightness;
    private Boolean redLedStatus;
    
    // === DHT配置 ===
    private Long dhtReadInterval;  // DHT读取间隔(毫秒)
    
    // === 摄像头配置 ===
    private Integer framesize;     // 分辨率
    private Integer brightness;    // 亮度(-2~2)
    private Integer contrast;      // 对比度(-2~2)
    private Integer saturation;    // 饱和度(-2~2)
    private Integer quality;       // JPEG质量(10-63)
    private Integer specialEffect; // 特效(0-6)
    
    // === 网络配置 ===
    private String wifiSsid;
    private String wifiPassword;   // 密码回显
    private String mqttServer;
    private Integer mqttPort;
    private String uploadUrl;
    
    // === 时间 ===
    private String time;
}
