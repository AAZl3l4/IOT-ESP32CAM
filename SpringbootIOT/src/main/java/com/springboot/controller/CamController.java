package com.springboot.controller;

import com.springboot.pojo.Query.*;
import com.springboot.utils.Result;
import com.springboot.service.CamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * ESP32-CAM 控制接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/mqtt")
@CrossOrigin(origins = "*")
public class CamController {

    @Autowired
    private CamService camService;

    /**
     * 触发拍照 (1080p高清)
     */
    @PostMapping("/capture/{clientId}")
    public Result<String> capture(@PathVariable @NotBlank String clientId) {
        String cmdId = camService.triggerCapture(clientId);
        return Result.success("拍照指令已发送", cmdId);
    }

    /**
     * 控制LED开关
     */
    @PostMapping("/led/{clientId}")
    public Result<String> led(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid LedRequest request) {
        String cmdId = camService.controlLed(clientId, request.getValue());
        return Result.success("LED控制指令已发送", cmdId);
    }

    /**
     * 设置LED亮度
     */
    @PostMapping("/led-brightness/{clientId}")
    public Result<String> ledBrightness(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid LedBrightnessRequest request) {
        String cmdId = camService.setLedBrightness(clientId, request.getBrightness());
        return Result.success("LED亮度指令已发送", cmdId);
    }

    /**
     * 设置摄像头参数
     */
    @PostMapping("/param/{clientId}")
    public Result<String> setParam(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid CameraParamRequest request) {
        String cmdId = camService.setCameraParam(clientId, request.getName(), request.getValue());
        return Result.success("参数设置指令已发送", cmdId);
    }

    /**
     * 获取设备状态
     */
    @GetMapping("/status/{clientId}")
    public Result<Map<String, Object>> getStatus(@PathVariable @NotBlank String clientId) {
        Map<String, Object> status = camService.getDeviceStatus(clientId);
        return Result.success(status);
    }

    /**
     * 设置视频流分辨率
     */
    @PostMapping("/stream-resolution/{clientId}")
    public Result<String> setStreamResolution(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid ResolutionRequest request) {
        String cmdId = camService.setStreamResolution(clientId, request.getFramesize());
        String resolution = request.getFramesize() == 7 ? "480p" 
            : request.getFramesize() == 11 ? "720p" 
            : request.getFramesize() == 14 ? "1080p" : "未知";
        return Result.success("视频流分辨率设置为" + resolution, cmdId);
    }

    /**
     * 设置WiFi配置
     */
    @PostMapping("/config/wifi/{clientId}")
    public Result<String> setWiFiConfig(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid WiFiConfigRequest request) {
        String cmdId = camService.setWiFiConfig(clientId, request.getSsid(), request.getPassword());
        return Result.success("WiFi配置已保存，设备将重启", cmdId);
    }

    /**
     * 设置MQTT配置
     */
    @PostMapping("/config/mqtt/{clientId}")
    public Result<String> setMQTTConfig(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid MqttConfigRequest request) {
        String mqttClientId = request.getMqttClientId() != null 
            ? request.getMqttClientId() : clientId;
        String cmdId = camService.setMQTTConfig(clientId, request.getServer(), 
            request.getPort(), mqttClientId);
        return Result.success("MQTT配置已保存，设备将重启", cmdId);
    }

    /**
     * 设置上传URL
     */
    @PostMapping("/config/upload-url/{clientId}")
    public Result<String> setUploadUrl(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid UploadUrlRequest request) {
        String cmdId = camService.setUploadUrl(clientId, request.getUrl());
        return Result.success("上传URL已保存", cmdId);
    }

    /**
     * 重置为默认配置
     */
    @PostMapping("/config/reset/{clientId}")
    public Result<String> resetConfig(@PathVariable @NotBlank String clientId) {
        String cmdId = camService.resetConfig(clientId);
        return Result.success("配置已重置，设备将重启", cmdId);
    }

    /**
     * 查询设备配置
     */
    @GetMapping("/config/{clientId}")
    public Result<String> getConfig(@PathVariable @NotBlank String clientId) {
        String cmdId = camService.getConfig(clientId);
        return Result.success("配置查询指令已发送，请查看MQTT响应", cmdId);
    }

    /**
     * 图片上传接口 (ESP32调用)
     */
    @PostMapping(value = "/cam/upload", consumes = "multipart/form-data")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName) {
        try {
            // 创建photos目录
            Path photosDir = Paths.get("photos");
            Files.createDirectories(photosDir);
            
            // 保存文件
            Path filePath = photosDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            long fileSize = Files.size(filePath);
            log.info("图片上传成功: {}, 大小: {} bytes", fileName, fileSize);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("fileSize", fileSize);
            result.put("path", filePath.toAbsolutePath().toString());
            
            return Result.success("上传成功", result);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有API文档
     */
    @GetMapping("/api-docs")
    public Result<Map<String, Object>> apiDocs() {
        Map<String, String> apis = new HashMap<>();
        apis.put("POST /mqtt/capture/{clientId}", "触发拍照(1080p)");
        apis.put("POST /mqtt/led/{clientId}", "LED开关控制 {\"value\":0/1}");
        apis.put("POST /mqtt/led-brightness/{clientId}", "LED亮度调节 {\"brightness\":0-255}");
        apis.put("POST /mqtt/param/{clientId}", "摄像头参数设置 {\"name\":\"xxx\",\"value\":0}");
        apis.put("GET /mqtt/status/{clientId}", "查询设备状态");
        apis.put("POST /mqtt/stream-resolution/{clientId}", "设置视频流分辨率 {\"framesize\":7/11/14}");
        apis.put("POST /mqtt/config/wifi/{clientId}", "设置WiFi {\"ssid\":\"xxx\",\"password\":\"xxx\"}");
        apis.put("POST /mqtt/config/mqtt/{clientId}", "设置MQTT {\"server\":\"xxx\",\"port\":1883}");
        apis.put("POST /mqtt/config/upload-url/{clientId}", "设置上传URL {\"url\":\"http://...\"}");
        apis.put("POST /mqtt/config/reset/{clientId}", "重置为默认配置");
        apis.put("GET /mqtt/config/{clientId}", "查询设备配置");
        apis.put("POST /mqtt/cam/upload", "图片上传(ESP32内部调用)");
        
        Map<String, Object> data = new HashMap<>();
        data.put("version", "2.0.0");
        data.put("streamUrl", "http://{esp32-ip}/stream");
        data.put("apis", apis);
        
        return Result.success("ESP32-CAM MQTT IoT API", data);
    }
}
