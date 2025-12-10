package com.springboot.controller;

import com.springboot.service.DeviceStatusHistoryService;
import com.springboot.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备状态历史数据控制器
 */
@RestController
@RequestMapping("/mqtt/status-history")
@CrossOrigin(origins = "*")
public class DeviceStatusHistoryController {
    
    @Autowired
    private DeviceStatusHistoryService deviceStatusHistoryService;
    
    /**
     * 获取设备状态历史图表数据（rssi/freeHeap）
     */
    @GetMapping("/chart/{clientId}")
    public Result<Map<String, Object>> getChartData(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "30") int limit) {
        if (limit > 100) limit = 100;
        Map<String, Object> data = deviceStatusHistoryService.getChartData(clientId, limit);
        return Result.success(data);
    }
}
