package com.springboot.controller;

import com.springboot.pojo.AutomationConfig;
import com.springboot.utils.Result;
import com.springboot.service.AutomationConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 自动化配置控制器
 * 提供自动化配置的查询和保存API
 */
@Slf4j
@RestController
@RequestMapping("/automation")
@CrossOrigin(origins = "*")
public class AutomationConfigController {
    
    @Autowired
    private AutomationConfigService automationConfigService;
    
    /**
     * 获取设备自动化配置
     * @param clientId 设备ID
     * @return 自动化配置
     */
    @GetMapping("/config/{clientId}")
    public Result<AutomationConfig> getConfig(@PathVariable String clientId) {
        AutomationConfig config = automationConfigService.getConfig(clientId);
        return Result.success("获取成功", config);
    }
    
    /**
     * 保存设备自动化配置
     * @param clientId 设备ID
     * @param config 配置内容
     * @return 保存结果
     */
    @PostMapping("/config/{clientId}")
    public Result<Void> saveConfig(@PathVariable String clientId, @RequestBody AutomationConfig config) {
        config.setClientId(clientId);
        automationConfigService.saveConfig(config);
        log.info("保存自动化配置: clientId={}, enabled={}", clientId, config.getEnabled());
        return Result.success("保存成功", null);
    }
}
