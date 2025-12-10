package com.springboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备状态历史实体类
 * 用于存储rssi和freeHeap等监控数据
 */
@Data
@TableName("device_status_history")
public class DeviceStatusHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("client_id")
    private String clientId;
    
    /** WiFi信号强度(dBm) */
    private Integer rssi;
    
    /** 空闲内存(bytes) */
    @TableField("free_heap")
    private Integer freeHeap;
    
    /** 运行时间(秒) */
    private Long uptime;
    
    @TableField("create_time")
    private LocalDateTime createTime;
}
