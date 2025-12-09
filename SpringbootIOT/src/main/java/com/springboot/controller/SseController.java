package com.springboot.controller;

import com.springboot.service.CamService;
import com.springboot.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE推送控制器
 * 用于建立SSE连接，实时推送温湿度数据
 */
@Slf4j
@RestController
@RequestMapping("/mqtt/sse")
@CrossOrigin(origins = "*")
public class SseController {
    
    @Autowired
    private SseService sseService;
    
    @Autowired
    private CamService camService;
    
    /**
     * 建立SSE连接
     * 前端通过 EventSource 连接此端点
     * 连接建立后自动请求设备配置
     */
    @GetMapping(value = "/dht/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDht(@PathVariable String clientId) {
        log.info("建立SSE连接请求: clientId={}", clientId);
        SseEmitter emitter = sseService.createConnection(clientId);
        
        // 异步发送get_config命令，让ESP32上报配置
        new Thread(() -> {
            try {
                Thread.sleep(500); // 等待SSE连接稳定
                log.info("SSE连接建立，发送get_config命令到设备: {}", clientId);
                camService.getConfig(clientId);
            } catch (Exception e) {
                log.error("发送get_config失败: {}", e.getMessage());
            }
        }).start();
        
        return emitter;
    }
}
