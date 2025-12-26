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
    
    @Autowired
    private com.springboot.service.SseService sseService;
    
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
        
        // SSE推送新日志
        sseService.pushOperationLog(clientId, operation, operationLog.getOperationDesc(), "pending", null);
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
            
            // SSE推送更新后的日志
            sseService.pushOperationLog(operationLog.getClientId(), operationLog.getOperation(), 
                    operationLog.getOperationDesc(), operationLog.getResult(), message);
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
    
    @Override
    public void logVoiceCommand(String clientId, String info, boolean success) {
        // 从info解析操作描述：去掉"语音控制: "前缀，提取操作描述
        String operationDesc = info;
        if (info != null && info.startsWith("语音控制:")) {
            operationDesc = info.substring("语音控制:".length()).trim();
        } else if (info != null && info.startsWith("语音控制: ")) {
            operationDesc = info.substring("语音控制: ".length()).trim();
        }
        
        OperationLog operationLog = new OperationLog();
        operationLog.setClientId(clientId);
        operationLog.setOperation("voice_cmd");  // 标记为语音控制
        operationLog.setOperationDesc("🎤 语音: " + operationDesc);
        operationLog.setCmdId(0L);
        operationLog.setResult(success ? "success" : "failed");
        operationLog.setResultMsg(operationDesc);
        operationLog.setCreateTime(LocalDateTime.now());
        
        operationLogMapper.insert(operationLog);
        log.info("记录语音控制日志: clientId={}, desc={}", clientId, operationDesc);
        
        // SSE推送日志
        sseService.pushOperationLog(clientId, "voice_cmd", 
                "🎤 语音: " + operationDesc, 
                success ? "success" : "failed", operationDesc);
    }
    
    @Override
    public void logAutoCommand(String clientId, String operation, String description) {
        OperationLog operationLog = new OperationLog();
        operationLog.setClientId(clientId);
        operationLog.setOperation("auto_cmd");  // 标记为自动化执行
        operationLog.setOperationDesc("🤖 自动化: " + description);
        operationLog.setCmdId(0L);
        operationLog.setResult("success");
        operationLog.setResultMsg(description);
        operationLog.setCreateTime(LocalDateTime.now());
        
        operationLogMapper.insert(operationLog);
        log.info("记录自动化日志: clientId={}, op={}, desc={}", clientId, operation, description);
        
        // SSE推送日志
        sseService.pushOperationLog(clientId, "auto_cmd", 
                "🤖 自动化: " + description, 
                "success", description);
    }
}
