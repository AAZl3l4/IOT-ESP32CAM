package com.springboot.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DHT温湿度面板数据响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DhtDashboardResponse {
    /** 当前温度 */
    private Double temperature;
    
    /** 当前湿度 */
    private Double humidity;
    
    /** 更新时间 (HH:mm:ss) */
    private String updateTime;
    
    /** 图表时间标签 */
    private List<String> labels;
    
    /** 温度历史数据 */
    private List<Double> temperatures;
    
    /** 湿度历史数据 */
    private List<Double> humidities;
}
