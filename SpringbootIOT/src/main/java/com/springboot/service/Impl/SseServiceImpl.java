package com.springboot.service.Impl;

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
    public void pushDhtData(String clientId, double temperature, double humidity, Boolean lightDark) {
        // 创建DhtData对象用于推送
        DhtDataPush data = new DhtDataPush();
        data.setClientId(clientId);
        data.setTemperature(temperature);
        data.setHumidity(humidity);
        data.setLightDark(lightDark);
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
    
    @Override
    public void pushDeviceStatus(String clientId, Object status) {
        broadcastAsync("status", status);
    }
    
    private void broadcastAsync(String eventName, Object data) {
        // 使用线程安全的方式广播，捕获所有异常防止日志刷屏
        new Thread(() -> {
            for (SseEmitter emitter : emitterList) {
                try {
                    sendSafe(emitter, eventName, data);
                } catch (Throwable ignored) {
                    // 完全静默处理，防止任何异常泄露
                }
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
            // 使用debug级别，不会输出到控制台
        } catch (IllegalStateException e) {
            // emitter已关闭
            emitterList.remove(emitter);
        } catch (Exception e) {
            // 其他异常也静默处理
            emitterList.remove(emitter);
        }
    }
    
    @Override
    public void pushCaptureResult(String clientId, String cmdId, String imageFile) {
        CaptureResultPush data = new CaptureResultPush();
        data.setClientId(clientId);
        data.setCmdId(cmdId);
        data.setImageFile(imageFile);
        data.setTime(LocalDateTime.now().format(timeFormatter));
        
        broadcastAsync("capture", data);
        log.info("SSE推送拍照结果: clientId={}, imageFile={}", clientId, imageFile);
    }
    
    @Override
    public void pushAiResponse(String sessionId, String taskId, String response, String imageFile) {
        AiResponsePush data = new AiResponsePush();
        data.setSessionId(sessionId);
        data.setTaskId(taskId);
        data.setResponse(response);
        data.setImageFile(imageFile);
        data.setTime(LocalDateTime.now().format(timeFormatter));
        
        broadcastAsync("ai-response", data);
        log.info("SSE推送AI响应: sessionId={}, taskId={}", sessionId, taskId);
    }

    // 内部类：拍照结果推送对象
    @Data
    private static class CaptureResultPush {
        private String clientId;
        private String cmdId;
        private String imageFile;
        private String time;
    }

    // 内部类：AI响应推送对象
    @Data
    private static class AiResponsePush {
        private String sessionId;
        private String taskId;
        private String response;
        private String imageFile;
        private String time;
    }

    // 内部类：DHT数据推送对象
    @Data
    private static class DhtDataPush {
        private String clientId;
        private Double temperature;
        private Double humidity;
        private Boolean lightDark;    // 是否暗
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
