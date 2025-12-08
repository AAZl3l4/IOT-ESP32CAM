package com.springboot.controller;

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
    
    /**
     * 建立SSE连接
     * 前端通过 EventSource 连接此端点
     */
    @GetMapping(value = "/dht/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDht(@PathVariable String clientId) {
        log.info("建立SSE连接请求: clientId={}", clientId);
        return sseService.createConnection(clientId);
    }
}
