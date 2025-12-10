package com.springboot.service.Impl;

import com.springboot.configuration.MqttGateway;
import com.springboot.pojo.DeviceConfig;
import com.springboot.pojo.DhtData;
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
    
    @Autowired
    private com.springboot.service.DhtDataService dhtDataService;
    
    @Autowired
    private com.springboot.service.SseService sseService;
    
    @Autowired
    private com.springboot.service.DeviceStatusHistoryService deviceStatusHistoryService;

    /**
     * 设备状态缓存 - 存储最新的设备状态
     * Key: clientId, Value: DeviceConfig
     */
    private final ConcurrentHashMap<String, DeviceConfig> deviceStatusCache = new ConcurrentHashMap<>();

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
            DeviceConfig status = JsonUtil.fromJson(json, DeviceConfig.class);
            if (status != null) {
                status.setLastUpdateTime(System.currentTimeMillis());
                deviceStatusCache.put(status.getClientId(), status);
                
                // 保存状态历史到数据库
                deviceStatusHistoryService.save(
                    status.getClientId(), 
                    status.getRssi(), 
                    status.getFreeHeap(), 
                    status.getUptime()
                );
                
                // 推送状态数据到前端
                sseService.pushDeviceStatus(status.getClientId(), status);
                
                log.info("设备状态更新: clientId={}, uptime={}s, freeHeap={}, rssi={}", 
                         status.getClientId(), status.getUptime(), 
                         status.getFreeHeap(), status.getRssi());
            }
        }
        // 处理DHT22温湿度数据上报
        else if (topic.endsWith("/dht")) {
            try {
                DhtData dhtData = JsonUtil.fromJson(json, DhtData.class);
                if (dhtData != null && dhtData.getClientId() != null) {
                    dhtDataService.save(dhtData.getClientId(), dhtData.getTemperature(), dhtData.getHumidity());
                    // SSE实时推送到前端
                    sseService.pushDhtData(dhtData.getClientId(), dhtData.getTemperature(), dhtData.getHumidity());
                    log.info("温湿度数据: clientId={}, 温度={}℃, 湿度={}%", 
                             dhtData.getClientId(), dhtData.getTemperature(), dhtData.getHumidity());
                }
            } catch (Exception e) {
                log.error("解析DHT数据失败: {}", e.getMessage());
            }
        }
        // 处理设备配置上报
        else if (topic.endsWith("/config")) {
            try {
                DeviceConfig config = JsonUtil.fromJson(json, DeviceConfig.class);
                if (config != null) {
                    log.info("设备配置上报: clientId={}, ledBrightness={}, dhtInterval={}", 
                            config.getClientId(), config.getLedBrightness(), config.getDhtInterval());
                    // SSE实时推送到前端
                    sseService.pushDeviceConfig(config.getClientId(), config);
                }
            } catch (Exception e) {
                log.error("解析Config数据失败: {}", e.getMessage());
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
     * 控制红色指示灯开关
     */
    @Override
    public String controlRedLed(String clientId, int value) {
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "red_led", "val", value));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送红色指示灯指令: clientId={}, cmdId={}, value={}", clientId, id, value);
        // 记录操作日志
        operationLogService.log(clientId, "red_led", id, value);
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
    public com.springboot.pojo.vo.DeviceStatusResponse getDeviceStatus(String clientId) {
        DeviceConfig status = deviceStatusCache.get(clientId);
        
        if (status != null) {
            // 计算设备是否在线(超过60秒未上报则视为离线)
            long timeDiff = System.currentTimeMillis() - status.getLastUpdateTime();
            
            return com.springboot.pojo.vo.DeviceStatusResponse.builder()
                    .found(true)
                    .clientId(status.getClientId())
                    .uptime(status.getUptime())
                    .freeHeap(status.getFreeHeap())
                    .rssi(status.getRssi())
                    .ledStatus(status.getLedStatus())
                    .ledBrightness(status.getLedBrightness())
                    .framesize(status.getFramesize())
                    .lastUpdateTime(status.getLastUpdateTime())
                    .online(timeDiff < 60000)
                    .build();
        } else {
            return com.springboot.pojo.vo.DeviceStatusResponse.builder()
                    .found(false)
                    .message("未找到该设备状态信息")
                    .build();
        }
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

    /**
     * 设置DHT读取间隔
     */
    @Override
    public String setDhtInterval(String clientId, int interval) {
        // 限制范围 1000-60000 毫秒
        if (interval < 1000) interval = 1000;
        if (interval > 60000) interval = 60000;
        
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "set_dht_interval", "val", interval));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送DHT间隔设置指令: clientId={}, cmdId={}, interval={}ms", clientId, id, interval);
        // 记录操作日志
        operationLogService.log(clientId, "set_dht_interval", id, interval);
        return "cmd queued " + id;
    }
    
    /**
     * 设置状态上报间隔
     */
    @Override
    public String setStatusInterval(String clientId, int interval) {
        // 限制范围 1000-300000 毫秒(1秒-5分钟)
        if (interval < 1000) interval = 1000;
        if (interval > 300000) interval = 300000;
        
        long id = generateCmdId();
        String json = JsonUtil.toJson(Map.of("id", id, "op", "set_status_interval", "val", interval));
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        log.info("发送状态上报间隔设置指令: clientId={}, cmdId={}, interval={}ms", clientId, id, interval);
        // 记录操作日志
        operationLogService.log(clientId, "set_status_interval", id, interval);
        return "cmd queued " + id;
    }
}
