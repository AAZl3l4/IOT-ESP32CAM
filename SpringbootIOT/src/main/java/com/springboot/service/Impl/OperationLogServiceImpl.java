package com.springboot.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.springboot.mapper.OperationLogMapper;
import com.springboot.pojo.OperationLog;
import com.springboot.service.OperationLogService;
import com.springboot.utils.OperationDesc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Override
    public void log(String clientId, String operation, Long cmdId, Integer value) {
        OperationLog operationLog = new OperationLog();
        operationLog.setClientId(clientId);
        operationLog.setOperation(operation);
        operationLog.setOperationDesc(OperationDesc.getFullDesc(operation, value));
        operationLog.setCmdId(cmdId);
        operationLog.setResult("pending");
        operationLog.setCreateTime(LocalDateTime.now());
        
        operationLogMapper.insert(operationLog);
        log.info("记录操作日志: clientId={}, operation={}, cmdId={}", clientId, operation, cmdId);
    }
    
    @Override
    public void updateResult(Long cmdId, boolean success, String message) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getCmdId, cmdId);
        
        OperationLog operationLog = operationLogMapper.selectOne(wrapper);
        if (operationLog != null) {
            operationLog.setResult(success ? "success" : "failed");
            operationLog.setResultMsg(message);
            operationLogMapper.updateById(operationLog);
            log.info("更新操作结果: cmdId={}, result={}, msg={}", cmdId, success ? "成功" : "失败", message);
        } else {
            log.warn("【警告】未找到操作日志记录: cmdId={}", cmdId);
        }
    }
    
    @Override
    public List<OperationLog> getLatestLogs(int limit) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OperationLog::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return operationLogMapper.selectList(wrapper);
    }
    
    @Override
    public List<OperationLog> getLogsByClientId(String clientId, int limit) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getClientId, clientId);
        wrapper.orderByDesc(OperationLog::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return operationLogMapper.selectList(wrapper);
    }
}
