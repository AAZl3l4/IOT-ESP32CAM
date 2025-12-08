package com.springboot.service.Impl;

import com.springboot.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE推送服务实现
 * 用于实时推送温湿度数据到前端
 */
@Slf4j
@Service
public class SseServiceImpl implements SseService {
    
    // 存储所有SSE连接，key为clientId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @Override
    public SseEmitter createConnection(String clientId) {
        // 创建30分钟超时的SSE连接
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 设置回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", clientId);
            emitters.remove(clientId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", clientId);
            emitters.remove(clientId);
        });
        
        emitter.onError(e -> {
            log.error("SSE连接错误: {}, error: {}", clientId, e.getMessage());
            emitters.remove(clientId);
        });
        
        // 存储连接
        emitters.put(clientId, emitter);
        log.info("SSE连接建立: {}, 当前连接数: {}", clientId, emitters.size());
        
        // 发送初始连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\",\"clientId\":\"" + clientId + "\"}"));
        } catch (IOException e) {
            log.error("发送连接消息失败: {}", e.getMessage());
        }
        
        return emitter;
    }
    
    @Override
    public void pushDhtData(String clientId, double temperature, double humidity) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                String data = String.format(
                        "{\"temperature\":%.2f,\"humidity\":%.2f,\"time\":\"%tT\"}",
                        temperature, humidity, System.currentTimeMillis());
                emitter.send(SseEmitter.event()
                        .name("dht")
                        .data(data));
                log.debug("SSE推送DHT数据: clientId={}, temp={}, humidity={}", clientId, temperature, humidity);
            } catch (IOException e) {
                log.error("SSE推送失败: {}", e.getMessage());
                emitters.remove(clientId);
            }
        }
    }
    
    @Override
    public void removeConnection(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE连接已移除: {}", clientId);
        }
    }
}
