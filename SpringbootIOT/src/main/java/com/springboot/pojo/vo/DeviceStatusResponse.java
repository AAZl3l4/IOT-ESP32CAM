package com.springboot.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 设备状态响应VO
 * 替代原有的Map<String, Object>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusResponse {
    /** 是否找到设备 */
    private Boolean found;
    
    /** 设备ID */
    private String clientId;
    
    /** 运行时间(秒) */
    private Long uptime;
    
    /** 空闲内存(bytes) */
    private Integer freeHeap;
    
    /** WiFi信号强度(dBm) */
    private Integer rssi;
    
    /** LED状态 */
    private Boolean ledStatus;
    
    /** LED亮度 */
    private Integer ledBrightness;
    
    /** 红色指示灯状态 */
    private Boolean redLedStatus;
    
    /** 分辨率代码 */
    private Integer framesize;
    
    /** 最后更新时间 */
    private Long lastUpdateTime;
    
    /** 是否在线 */
    private Boolean online;
    
    /** 未找到时的消息 */
    private String message;
}
