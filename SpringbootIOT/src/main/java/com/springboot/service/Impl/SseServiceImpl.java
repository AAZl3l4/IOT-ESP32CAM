package com.springboot.service.Impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.springboot.pojo.DhtData;
import com.springboot.pojo.OperationLog;
import com.springboot.service.SseService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
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
        log.info("SSE连接建立: {}, 当前连接数: {}", clientId, emitterList.size());
        
        Runnable removeEmitter = () -> {
            emitterList.remove(emitter);
            log.debug("SSE连接移除, 剩余: {}", emitterList.size());
        };
        
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(e -> removeEmitter.run());
        
        // 发送连接成功消息
        sendSafe(emitter, "connected", new ConnectedMessage("connected", clientId));
        
        return emitter;
    }
    
    @Override
    public void pushDhtData(String clientId, double temperature, double humidity) {
        // 创建DhtData对象用于推送（不保存到数据库）
        DhtDataPush data = new DhtDataPush();
        data.setClientId(clientId);
        data.setTemperature(temperature);
        data.setHumidity(humidity);
        data.setTime(LocalDateTime.now().format(timeFormatter));
        
        broadcastAsync("dht", data);
    }
    
    @Override
    public void pushOperationLog(String clientId, String operation, String operationDesc, 
                                  String result, String resultMsg) {
        OperationLogPush log = new OperationLogPush();
        log.setClientId(clientId);
        log.setOperation(operation);
        log.setOperationDesc(operationDesc);
        log.setResult(result);
        log.setResultMsg(resultMsg != null ? resultMsg : "");
        log.setTime(LocalDateTime.now().format(timeFormatter));
        
        broadcastAsync("log", log);
    }
    
    @Override
    public void pushDeviceConfig(String clientId, Object config) {
        broadcastAsync("config", config);
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
        } catch (java.io.IOException e) {
            // 客户端断开连接，静默移除（这是正常现象）
            emitterList.remove(emitter);
            log.debug("SSE客户端已断开，移除连接");
        } catch (Exception e) {
            // 其他异常也静默处理
            emitterList.remove(emitter);
            log.debug("SSE发送失败，移除连接: {}", e.getMessage());
        }
    }
    
    // 内部类：DHT数据推送对象
    @Data
    private static class DhtDataPush {
        private String clientId;
        private Double temperature;
        private Double humidity;
        private String time; // HH:mm:ss格式
    }
    
    // 内部类：操作日志推送对象
    @Data
    private static class OperationLogPush {
        private String clientId;
        private String operation;
        private String operationDesc;
        private String result;
        private String resultMsg;
        private String time; // HH:mm:ss格式
    }
    
    // 内部类：连接成功消息
    @Data
    private static class ConnectedMessage {
        private String status;
        private String clientId;
        
        public ConnectedMessage(String status, String clientId) {
            this.status = status;
            this.clientId = clientId;
        }
    }
}
