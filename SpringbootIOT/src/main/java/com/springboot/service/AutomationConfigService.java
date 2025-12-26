package com.springboot.service;

import com.springboot.pojo.AutomationConfig;

/**
 * 自动化服务接口
 * 负责智能自动化控制逻辑
 */
public interface AutomationConfigService {
    
    /**
     * 获取设备自动化配置
     * @param clientId 设备ID
     * @return 配置
     */
    AutomationConfig getConfig(String clientId);
    
    /**
     * 保存自动化配置
     * @param config 配置
     */
    void saveConfig(AutomationConfig config);
    
    /**
     * 检查DHT数据并执行自动化（温度、湿度、光照）
     * @param clientId 设备ID
     * @param temperature 温度
     * @param humidity 湿度
     * @param lightDark 是否暗
     */
    void checkAndExecuteDht(String clientId, double temperature, double humidity, Boolean lightDark);
    
    /**
     * 检查设备状态并执行自动化（内存、信号）
     * @param clientId 设备ID
     * @param freeHeap 空闲内存
     * @param rssi 信号强度
     */
    void checkAndExecuteStatus(String clientId, int freeHeap, int rssi);
    
    /**
     * 记录手动操作，开始暂停期
     * @param clientId 设备ID
     */
    void recordManualOperation(String clientId);
    
    /**
     * 检查是否在手动暂停期
     * @param clientId 设备ID
     * @return 是否在暂停期
     */
    boolean isInManualPause(String clientId);
}
