package com.springboot.service;

import com.springboot.pojo.DeviceStatusHistory;
import com.springboot.pojo.vo.StatusChartResponse;

import java.util.List;

/**
 * 设备状态历史服务接口
 */
public interface DeviceStatusHistoryService {
    
    /**
     * 保存设备状态
     */
    void save(String clientId, int rssi, int freeHeap, long uptime);
    
    /**
     * 获取最近N条状态记录
     */
    List<DeviceStatusHistory> getLatest(String clientId, int limit);
    
    /**
     * 获取图表数据（时间标签+rssi+freeHeap数组）
     */
    StatusChartResponse getChartData(String clientId, int limit);
}
