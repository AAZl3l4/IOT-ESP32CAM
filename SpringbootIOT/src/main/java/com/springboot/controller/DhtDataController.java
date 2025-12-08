package com.springboot.controller;

import com.springboot.service.DhtDataService;
import com.springboot.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DHT22温湿度数据控制器
 */
@RestController
@RequestMapping("/mqtt/dht")
@CrossOrigin(origins = "*")
public class DhtDataController {
    
    @Autowired
    private DhtDataService dhtDataService;
    
    /**
     * 获取温湿度面板数据(当前值 + 图表数据)
     */
    @GetMapping("/dashboard/{clientId}")
    public Result<Map<String, Object>> getDashboard(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "30") int chartLimit) {
        if (chartLimit > 100) chartLimit = 100;
        Map<String, Object> data = dhtDataService.getDashboardData(clientId, chartLimit);
        return Result.success(data);
    }
}
