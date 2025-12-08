package com.springboot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备状态数据传输对象
 * 用于存储ESP32设备上报的状态信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatus {
    /**
     * 设备客户端ID
     */
    private String clientId;
    
    /**
     * 设备运行时间(秒)
     */
    private long uptime;
    
    /**
     * 空闲堆内存(bytes)
     */
    private int freeHeap;
    
    /**
     * WiFi信号强度(dBm)
     */
    private int rssi;
    
    /**
     * LED开关状态
     */
    private boolean ledStatus;
    
    /**
     * LED亮度(0-255)
     */
    private int ledBrightness;
    
    /**
     * 当前分辨率
     */
    private int framesize;
    
    /**
     * 最后更新时间戳
     */
    private long lastUpdateTime;
}
