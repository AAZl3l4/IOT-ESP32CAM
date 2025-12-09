package com.springboot.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备配置实体
 * 封装ESP32设备的所有配置信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfig {
    /**
     * 设备ID
     */
    private String clientId;
    
    /**
     * WiFi SSID
     */
    private String wifiSsid;
    
    /**
     * WiFi IP地址
     */
    private String wifiIp;
    
    /**
     * WiFi信号强度(dBm)
     */
    private Integer rssi;
    
    /**
     * MQTT Broker地址
     */
    private String mqttBroker;
    
    /**
     * MQTT端口
     */
    private Integer mqttPort;
    
    /**
     * 摄像头分辨率(framesize)
     */
    private Integer framesize;
    
    /**
     * JPEG质量(0-63, 越小质量越高)
     */
    private Integer quality;
    
    /**
     * 亮度(-2到2)
     */
    private Integer brightness;
    
    /**
     * 对比度(-2到2)
     */
    private Integer contrast;
    
    /**
     * 饱和度(-2到2)
     */
    private Integer saturation;
    
    /**
     * 特效(0-6)
     */
    private Integer specialEffect;
    
    /**
     * 白平衡(0/1)
     */
    private Integer whiteBalance;
    
    /**
     * 自动白平衡增益(0/1)
     */
    private Integer awbGain;
    
    /**
     * WB模式(0-4)
     */
    private Integer wbMode;
    
    /**
     * 曝光控制(0/1)
     */
    private Integer exposureCtrl;
    
    /**
     * 自动曝光控制(0/1)
     */
    private Integer aec;
    
    /**
     * AEC值(0-1200)
     */
    private Integer aecValue;
    
    /**
     * 自动曝光调节(0-5)
     */
    private Integer aec2;
    
    /**
     * 自动增益控制(0/1)
     */
    private Integer gainCtrl;
    
    /**
     * AGC增益(0-30)
     */
    private Integer agcGain;
    
    /**
     * 增益上限(0-6)
     */
    private Integer gainceiling;
    
    /**
     * 黑像素校正(0/1)
     */
    private Integer bpc;
    
    /**
     * 白像素校正(0/1)
     */
    private Integer wpc;
    
    /**
     * 原始伽马(0/1)
     */
    private Integer rawGma;
    
    /**
     * 镜头校正(0/1)
     */
    private Integer lenc;
    
    /**
     * 水平镜像(0/1)
     */
    private Integer hmirror;
    
    /**
     * 垂直翻转(0/1)
     */
    private Integer vflip;
    
    /**
     * DCW使能(0/1)
     */
    private Integer dcw;
    
    /**
     * 色彩条(0/1)
     */
    private Integer colorbar;
    
    /**
     * LED开关状态
     */
    private Boolean ledStatus;
    
    /**
     * LED亮度(0-255)
     */
    private Integer ledBrightness;
    
    /**
     * 红色指示灯状态
     */
    private Boolean redLedStatus;
    
    /**
     * DHT读取间隔(毫秒)
     */
    private Integer dhtInterval;
    
    /**
     * 状态上报间隔(毫秒)
     */
    private Integer statusInterval;
    
    /**
     * 设备运行时间(秒)
     */
    private Long uptime;
    
    /**
     * 空闲堆内存(bytes)
     */
    private Integer freeHeap;
    
    /**
     * 最后更新时间戳
     */
    private Long lastUpdateTime;
}
