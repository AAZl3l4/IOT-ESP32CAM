package com.springboot.service;

import com.springboot.pojo.DhtData;

import java.util.List;
import java.util.Map;

/**
 * DHT22温湿度数据服务接口
 */
public interface DhtDataService {
    
    /**
     * 保存温湿度数据
     */
    void save(String clientId, double temperature, double humidity);
    
    /**
     * 获取最新一条数据
     */
    DhtData getLatest(String clientId);
    
    /**
     * 获取最近N条数据(用于图表)
     */
    List<DhtData> getLatestList(String clientId, int limit);
    
    /**
     * 获取最新数据和图表数据
     */
    Map<String, Object> getDashboardData(String clientId, int chartLimit);
}
