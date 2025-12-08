package com.springboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DHT22温湿度数据实体
 */
@Data
@TableName("dht_data")
public class DhtData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 设备ID
     */
    private String clientId;
    
    /**
     * 温度(℃)
     */
    private BigDecimal temperature;
    
    /**
     * 湿度(%)
     */
    private BigDecimal humidity;
    
    /**
     * 采集时间
     */
    private LocalDateTime createTime;
}
