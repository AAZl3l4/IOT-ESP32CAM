package com.springboot.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 设备状态图表数据响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChartResponse {
    /** 图表时间标签 */
    private List<String> labels;
    
    /** RSSI数据 (dBm) */
    private List<Integer> rssiData;
    
    /** 空闲内存数据 (KB) */
    private List<Integer> freeHeapData;
}
