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
}
