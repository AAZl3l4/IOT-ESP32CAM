package com.springboot.service;

import com.springboot.pojo.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    
    /**
     * 记录操作日志
     */
    void log(String clientId, String operation, Long cmdId, Integer value);
    
    /**
     * 更新操作结果
     */
    void updateResult(Long cmdId, boolean success, String message);
    
    /**
     * 获取最新N条日志
     */
    List<OperationLog> getLatestLogs(int limit);
    
    /**
     * 根据设备ID获取日志
     */
    List<OperationLog> getLogsByClientId(String clientId, int limit);
    
    /**
     * 记录语音控制日志（ESP32本地执行，无cmdId）
     * @param clientId 设备ID
     * @param info 语音控制描述（如"语音控制: LED开启"）
     * @param success 是否成功
     */
    void logVoiceCommand(String clientId, String info, boolean success);
    
    /**
     * 记录自动化执行日志
     * @param clientId 设备ID
     * @param operation 操作类型
     * @param description 操作描述
     */
    void logAutoCommand(String clientId, String operation, String description);
}
