package com.springboot.controller;

import com.springboot.pojo.OperationLog;
import com.springboot.service.OperationLogService;
import com.springboot.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/mqtt/logs")
@CrossOrigin(origins = "*")
public class OperationLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 获取最新操作日志(默认10条)
     */
    @GetMapping("/latest")
    public Result<List<OperationLog>> getLatestLogs(
            @RequestParam(defaultValue = "10") int limit) {
        if (limit > 100) limit = 100;  // 限制最大100条
        List<OperationLog> logs = operationLogService.getLatestLogs(limit);
        return Result.success(logs);
    }
    
    /**
     * 根据设备ID获取日志
     */
    @GetMapping("/client/{clientId}")
    public Result<List<OperationLog>> getLogsByClient(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "10") int limit) {
        if (limit > 100) limit = 100;
        List<OperationLog> logs = operationLogService.getLogsByClientId(clientId, limit);
        return Result.success(logs);
    }
}
