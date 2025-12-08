package com.springboot.service.Impl;

import com.springboot.configuration.MqttGateway;
import com.springboot.pojo.DeviceStatus;
import com.springboot.pojo.ResultDto;
import com.springboot.service.CamService;
import com.springboot.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CamServiceImpl implements CamService {
    @Autowired
    private MqttGateway mqttGateway;
    
    @Autowired
    private com.springboot.service.OperationLogService operationLogService;

    /**
     * 设备状态缓存 - 存储最新的设备状态
     * Key: clientId, Value: DeviceStatus
     */
    private final ConcurrentHashMap<String, DeviceStatus> deviceStatusCache = new ConcurrentHashMap<>();

    /**
     * 监听mqtt返回消息的方法
     * 处理来自ESP32的result和status消息
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<?> msg) {
        String topic = (String) msg.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String json = (String) msg.getPayload();
        log.info("MQTT收到消息 topic={}, payload={}", topic, json);

        // 处理执行结果
        if (topic.endsWith("/result")) {
            ResultDto r = JsonUtil.fromJson(json, ResultDto.class);
            if (r != null) {
                log.info("【调试】解析结果: id={}, ok={}, info='{}'", r.getId(), r.isOk(), r.getInfo());
                log.info("指令 {} 执行完成, 结果: ok={}, info={}", 
                         r.getId(), r.isOk(), r.getInfo());
                // 更新操作日志结果
                operationLogService.updateResult(r.getId(), r.isOk(), r.getInfo());
            } else {
                log.error("【错误】无法解析ResultDto: {}", json);
            }
        }
        // 处理设备状态上报
        else if (topic.endsWith("/status")) {
            DeviceStatus status = JsonUtil.fromJson(json, DeviceStatus.class);
            if (status != null) {
                status.setLastUpdateTime(System.currentTimeMillis());
                deviceStatusCache.put(status.getClientId(), status);
                log.info("设备状态更新: clientId={}, uptime={}s, freeHeap={}, rssi={}", 
                         status.getClientId(), status.getUptime(), 
                         status.getFreeHeap(), status.getRssi());
            }
        }
    }

    /**
     * 生成命令ID（确保在32位long范围内）
     * 格式：时间戳后6位 + 4位随机数 = 10位数字
     * 范围：100000000 - 9999999999
     */
    private long generateCmdId() {
        long timestamp = System.currentTimeMillis();
        // 修改为后5位，避免超过 ESP32 long (int32) 范围 (21亿)
        // 99999 * 10000 + 9999 = 999,999,999 < 2,147,483,647
        int timePart = (int)(timestamp % 100000); 
        int randomPart = (int)(Math.random() * 10000);  // 4位随机数
        return timePart * 10000L + randomPart;
    }

    /**
     * 触发拍照指令
     */
    @Override
    public String triggerCapture(String clientId) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "capture", "val", 0));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        // 记录操作日志
        operationLogService.log(clientId, "capture", id, 0);
        return "cmd queued " + id;
    }

    /**
     * 控制LED开关
     */
    @Override
    public String controlLed(String clientId, int value) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "led", "val", value));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送LED控制指令: clientId={}, cmdId={}, value={}", clientId, id, value);
        // 记录操作日志
        operationLogService.log(clientId, "led", id, value);
        return "cmd queued " + id;
    }

    /**
     * 设置LED亮度
     */
    @Override
    public String setLedBrightness(String clientId, int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("LED亮度值必须在0-255之间");
        }
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "led_brightness", "val", value));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送LED亮度指令: clientId={}, cmdId={}, brightness={}", clientId, id, value);
        // 记录操作日志
        operationLogService.log(clientId, "led_brightness", id, value);
        return "cmd queued " + id;
    }

    /**
     * 设置摄像头参数
     */
    @Override
    public String setCameraParam(String clientId, String param, int value) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", param, "val", value));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送摄像头参数指令: clientId={}, cmdId={}, param={}, value={}", 
                 clientId, id, param, value);
        // 记录操作日志
        operationLogService.log(clientId, param, id, value);
        return "cmd queued " + id;
    }

    /**
     * 获取设备状态
     */
    @Override
    public Map<String, Object> getDeviceStatus(String clientId) {
        DeviceStatus status = deviceStatusCache.get(clientId);
        
        Map<String, Object> result = new HashMap<>();
        if (status != null) {
            result.put("found", true);
            result.put("clientId", status.getClientId());
            result.put("uptime", status.getUptime());
            result.put("freeHeap", status.getFreeHeap());
            result.put("rssi", status.getRssi());
            result.put("ledStatus", status.isLedStatus());
            result.put("ledBrightness", status.getLedBrightness());
            result.put("framesize", status.getFramesize());
            result.put("lastUpdateTime", status.getLastUpdateTime());
            
            // 计算设备是否在线(超过60秒未上报则视为离线)
            long timeDiff = System.currentTimeMillis() - status.getLastUpdateTime();
            result.put("online", timeDiff < 60000);
        } else {
            result.put("found", false);
            result.put("message", "未找到该设备状态信息");
        }


        return result;
    }

    /**
     * 设置视频流分辨率
     */
    @Override
    public String setStreamResolution(String clientId, int framesize) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "framesize", "val", framesize));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送分辨率设置指令: clientId={}, cmdId={}, framesize={}", clientId, id, framesize);
        // 记录操作日志
        operationLogService.log(clientId, "framesize", id, framesize);
        return "cmd queued " + id;
    }

    /**
     * 设置WiFi配置
     */
    @Override
    public String setWiFiConfig(String clientId, String ssid, String password) {
        long id = generateCmdId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("op", "set_wifi");
        payload.put("ssid", ssid);
        payload.put("password", password);
        String json = JsonUtil.toJson(payload);
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送WiFi配置指令: clientId={}, cmdId={}", clientId, id);
        // 记录操作日志
        operationLogService.log(clientId, "set_wifi", id, 0);
        return "cmd queued " + id;
    }

    /**
     * 设置MQTT配置
     */
    @Override
    public String setMQTTConfig(String clientId, String server, int port, String mqttClientId) {
        long id = generateCmdId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("op", "set_mqtt");
        payload.put("server", server);
        payload.put("port", port);
        payload.put("clientId", mqttClientId);
        String json = JsonUtil.toJson(payload);
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送MQTT配置指令: clientId={}, cmdId={}", clientId, id);
        // 记录操作日志
        operationLogService.log(clientId, "set_mqtt", id, 0);
        return "cmd queued " + id;
    }

    /**
     * 设置上传URL
     */
    @Override
    public String setUploadUrl(String clientId, String url) {
        long id = generateCmdId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("op", "set_upload_url");
        payload.put("url", url);
        String json = JsonUtil.toJson(payload);
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送上传URL配置指令: clientId={}, cmdId={}, url={}", clientId, id, url);
        // 记录操作日志
        operationLogService.log(clientId, "set_upload_url", id, 0);
        return "cmd queued " + id;
    }

    /**
     * 重置配置
     */
    @Override
    public String resetConfig(String clientId) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "reset_config"));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送重置配置指令: clientId={}, cmdId={}", clientId, id);
        // 记录操作日志
        operationLogService.log(clientId, "reset_config", id, 0);
        return "cmd queued " + id;
    }

    /**
     * 查询配置
     */
    @Override
    public String getConfig(String clientId) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "get_config"));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送查询配置指令: clientId={}, cmdId={}", clientId, id);
        // 记录操作日志
        operationLogService.log(clientId, "get_config", id, 0);
        return "cmd queued " + id;
    }
}
