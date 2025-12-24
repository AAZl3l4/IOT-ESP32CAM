package com.springboot.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE推送服务接口
 */
public interface SseService {
    
    /**
     * 创建新的SSE连接
     */
    SseEmitter createConnection(String clientId);
    
    /**
     * 推送温湿度和光照数据
     */
    void pushDhtData(String clientId, double temperature, double humidity, Boolean lightDark);
    
    /**
     * 推送操作日志
     */
    void pushOperationLog(String clientId, String operation, String operationDesc, 
                          String result, String resultMsg);
    
    /**
     * 推送设备配置
     */
    void pushDeviceConfig(String clientId, Object config);
    
    /**
     * 推送设备状态（rssi/freeHeap实时数据）
     */
    void pushDeviceStatus(String clientId, Object status);
}
