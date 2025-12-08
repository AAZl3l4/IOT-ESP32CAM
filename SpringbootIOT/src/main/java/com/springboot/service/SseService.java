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
     * 推送温湿度数据
     */
    void pushDhtData(String clientId, double temperature, double humidity);
    
    /**
     * 移除连接
     */
    void removeConnection(String clientId);
}
