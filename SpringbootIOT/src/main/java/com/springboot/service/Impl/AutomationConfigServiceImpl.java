package com.springboot.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.springboot.configuration.MqttGateway;
import com.springboot.mapper.AutomationConfigMapper;
import com.springboot.pojo.AutomationConfig;
import com.springboot.service.AutomationConfigService;
import com.springboot.service.OperationLogService;
import com.springboot.service.SseService;
import com.springboot.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动化服务实现
 * 负责智能自动化控制逻辑
 */
@Slf4j
@Service
public class AutomationConfigServiceImpl implements AutomationConfigService {
    
    @Autowired
    private AutomationConfigMapper configMapper;
    
    @Autowired
    private MqttGateway mqttGateway;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private SseService sseService;
    
    /** 设备上次手动操作时间缓存 */
    private final ConcurrentHashMap<String, Long> manualOperationTime = new ConcurrentHashMap<>();
    
    /** 设备上次自动化执行状态缓存，避免重复执行 */
    private final ConcurrentHashMap<String, DeviceAutoState> deviceStates = new ConcurrentHashMap<>();
    
    /**
     * 设备自动化状态缓存类
     */
    private static class DeviceAutoState {
        boolean windowOpen = false; // 窗户打开状态
        boolean fanOn = false; // 风扇打开状态
        boolean ledOn = false; // 灯打开状态
        boolean redLedOn = false; // 指示灯状态
    }
    
    @Override
    public AutomationConfig getConfig(String clientId) {
        LambdaQueryWrapper<AutomationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutomationConfig::getClientId, clientId);
        return configMapper.selectOne(wrapper);
    }
    
    @Override
    public void saveConfig(AutomationConfig config) {
        LambdaQueryWrapper<AutomationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AutomationConfig::getClientId, config.getClientId());
        AutomationConfig existing = configMapper.selectOne(wrapper);
        
        if (existing != null) {
            config.setId(existing.getId());
            configMapper.updateById(config);
            log.info("更新自动化配置: clientId={}", config.getClientId());
        } else {
            configMapper.insert(config);
            log.info("新建自动化配置: clientId={}", config.getClientId());
        }
    }
    
    @Override
    public void checkAndExecuteDht(String clientId, double temperature, double humidity, Boolean lightDark) {
        AutomationConfig config = getConfig(clientId);
        
        // 检查是否启用自动化
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        
        // 检查是否在手动暂停期
        if (isInManualPause(clientId)) {
            log.debug("设备 {} 在手动暂停期，跳过自动化", clientId);
            return;
        }
        
        DeviceAutoState state = deviceStates.computeIfAbsent(clientId, k -> new DeviceAutoState());
        // ===== 温度控制 =====
        boolean needOpenWindow = false; // 是否需要开窗
        boolean needCloseWindow = false; // 是否需要关窗
        boolean needFanOn = false; // 是否需要开风扇
        boolean needFanOff = false; // 是否需要关风扇
        

        if (temperature > config.getTempHigh()) {
            // 高温：开窗+开风扇
            needOpenWindow = true;
            needFanOn = true;
        } else if (temperature < config.getTempLow()) {
            // 低温：关窗+关风扇
            needCloseWindow = true;
            needFanOff = true;
        }
        
        // ===== 湿度控制 =====
        if (humidity > config.getHumidHigh()) {
            // 高湿：开窗
            needOpenWindow = true;
        } else if (humidity < config.getHumidLow()) {
            // 低湿：关窗（但温度有需求时优先温度）
            if (!needOpenWindow) {
                needCloseWindow = true;
            }
        }
        
        // ===== 执行窗户控制 =====
        // 只要有一个需要开窗就开窗
        if (needOpenWindow && !state.windowOpen) {
            executeAutoCommand(clientId, "servo", 180, "开窗 (温度:" + temperature + "℃, 湿度:" + humidity + "%)");
            state.windowOpen = true;
        } else if (needCloseWindow && !needOpenWindow && state.windowOpen) {
            executeAutoCommand(clientId, "servo", 0, "关窗 (温度:" + temperature + "℃, 湿度:" + humidity + "%)");
            state.windowOpen = false;
        }
        
        // ===== 执行风扇控制 =====
        if (needFanOn && !state.fanOn) {
            executeAutoCommand(clientId, "fan_on", 0, "开风扇 (温度:" + temperature + "℃)");
            state.fanOn = true;
        } else if (needFanOff && !state.fanOn) {
            // 不需要额外关闭，保持状态
        } else if (needFanOff && state.fanOn && !needFanOn) {
            executeAutoCommand(clientId, "fan_off", 0, "关风扇 (温度:" + temperature + "℃)");
            state.fanOn = false;
        }
        
        // ===== 光照控制 =====
        if (lightDark != null) {
            if (lightDark && !state.ledOn) {
                // 暗 → 开灯
                executeAutoCommand(clientId, "led", 1, "开灯 (环境暗)");
                state.ledOn = true;
            } else if (!lightDark && state.ledOn) {
                // 亮 → 关灯
                executeAutoCommand(clientId, "led", 0, "关灯 (环境亮)");
                state.ledOn = false;
            }
        }
    }
    
    @Override
    public void checkAndExecuteStatus(String clientId, int freeHeap, int rssi) {
        AutomationConfig config = getConfig(clientId);
        
        // 检查是否启用自动化
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        
        // 检查是否在手动暂停期
        if (isInManualPause(clientId)) {
            return;
        }
        
        DeviceAutoState state = deviceStates.computeIfAbsent(clientId, k -> new DeviceAutoState());
        
        // 内存低或信号差 → 开红灯
        boolean needWarning = (freeHeap < config.getMemoryThreshold()) || (rssi < config.getRssiThreshold());
        
        if (needWarning && !state.redLedOn) {
            String reason = "";
            if (freeHeap < config.getMemoryThreshold()) {
                reason += "内存低(" + (freeHeap / 1024) + "KB) ";
            }
            if (rssi < config.getRssiThreshold()) {
                reason += "信号差(" + rssi + "dBm)";
            }
            executeAutoCommand(clientId, "red_led", 1, "开指示灯 (" + reason.trim() + ")");
            state.redLedOn = true;
        } else if (!needWarning && state.redLedOn) {
            executeAutoCommand(clientId, "red_led", 0, "关指示灯 (状态正常)");
            state.redLedOn = false;
        }
    }
    
    @Override
    public void recordManualOperation(String clientId) {
        manualOperationTime.put(clientId, System.currentTimeMillis());
        log.info("记录手动操作，设备 {} 进入暂停期", clientId);
    }
    
    @Override
    public boolean isInManualPause(String clientId) {
        Long lastManual = manualOperationTime.get(clientId);
        if (lastManual == null) {
            return false;
        }
        
        AutomationConfig config = getConfig(clientId);
        long elapsed = System.currentTimeMillis() - lastManual;
        return elapsed < config.getManualPauseMs();
    }
    
    /**
     * 执行自动化命令
     * @param clientId 设备ID
     * @param operation 操作类型
     * @param value 参数值
     * @param description 操作描述
     */
    private void executeAutoCommand(String clientId, String operation, int value, String description) {
        log.info("自动化执行: clientId={}, op={}, val={}, desc={}", clientId, operation, value, description);
        
        // 构建MQTT消息
        long cmdId = generateCmdId();
        String json;
        
        if ("fan_on".equals(operation) || "fan_off".equals(operation)) {
            json = JsonUtil.toJson(Map.of("id", cmdId, "op", operation));
        } else {
            json = JsonUtil.toJson(Map.of("id", cmdId, "op", operation, "val", value));
        }
        
        // 发送MQTT命令
        mqttGateway.send("cam/" + clientId + "/cmd", json);
        
        // 记录自动化日志
        operationLogService.logAutoCommand(clientId, operation, description);
    }
    
    /**
     * 生成命令ID
     */
    private long generateCmdId() {
        long timestamp = System.currentTimeMillis();
        int timePart = (int)(timestamp % 100000);
        int randomPart = (int)(Math.random() * 10000);
        return timePart * 10000L + randomPart;
    }
}
