package com.springboot.service.Impl;

import com.springboot.pojo.vo.DhtSseVo;
import com.springboot.pojo.vo.LogSseVo;
import com.springboot.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE推送服务实现
 */
@Slf4j
@Service
public class SseServiceImpl implements SseService {
    
    private final CopyOnWriteArrayList<SseEmitter> emitterList = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public SseEmitter createConnection(String clientId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        emitterList.add(emitter);
        log.info("SSE连接建立, 当前连接数: {}", emitterList.size());
        
        Runnable removeEmitter = () -> {
            emitterList.remove(emitter);
            log.debug("SSE连接移除, 剩余: {}", emitterList.size());
        };
        
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(e -> removeEmitter.run());
        
        // 发送连接成功消息
        sendSafe(emitter, "connected", new Object() {
            public final String status = "connected";
        });
        
        return emitter;
    }
    
    @Override
    public void pushDhtData(String clientId, double temperature, double humidity) {
        DhtSseVo vo = new DhtSseVo(
                clientId,
                temperature,
                humidity,
                LocalTime.now().format(timeFormatter)
        );
        broadcastAsync("dht", vo);
    }
    
    @Override
    public void pushOperationLog(String clientId, String operation, String operationDesc, 
                                  String result, String resultMsg) {
        LogSseVo vo = new LogSseVo(
                clientId,
                operation,
                operationDesc,
                result,
                resultMsg != null ? resultMsg : "",
                LocalTime.now().format(timeFormatter)
        );
        broadcastAsync("log", vo);
    }
    
    private void broadcastAsync(String eventName, Object data) {
        new Thread(() -> {
            for (SseEmitter emitter : emitterList) {
                sendSafe(emitter, eventName, data);
            }
        }).start();
    }
    
    private void sendSafe(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            emitterList.remove(emitter);
        }
    }
    
    @Override
    public void removeConnection(String clientId) {
        for (SseEmitter emitter : emitterList) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
        emitterList.clear();
    }
}
