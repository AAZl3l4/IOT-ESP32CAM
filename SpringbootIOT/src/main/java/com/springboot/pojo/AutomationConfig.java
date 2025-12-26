package com.springboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动化配置实体类
 * 存储设备智能控制的阈值配置
 */
@Data
@TableName("automation_config")
public class AutomationConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设备ID */
    @TableField("client_id")
    private String clientId;
    
    /** 自动化总开关 */
    private Boolean enabled;
    
    /** 高温阈值(℃) */
    @TableField("temp_high")
    private Integer tempHigh;
    
    /** 低温阈值(℃) */
    @TableField("temp_low")
    private Integer tempLow;
    
    /** 高湿阈值(%) */
    @TableField("humid_high")
    private Integer humidHigh;
    
    /** 低湿阈值(%) */
    @TableField("humid_low")
    private Integer humidLow;
    
    /** 内存阈值(bytes) */
    @TableField("memory_threshold")
    private Integer memoryThreshold;
    
    /** 信号阈值(dBm) */
    @TableField("rssi_threshold")
    private Integer rssiThreshold;
    
    /** 手动操作后暂停时间(ms) */
    @TableField("manual_pause_ms")
    private Long manualPauseMs;
    
    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
