package com.springboot.controller;

import com.springboot.pojo.Query.*;
import com.springboot.pojo.vo.DeviceStatusResponse;
import com.springboot.utils.Result;
import com.springboot.service.CamService;
import com.springboot.service.SseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Autowired
    private SseService sseService;
    
    /** 图片保存目录 */
    @Value("${photos-dir:photos}")
    private String photosDir;

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
     * 控制红色指示灯开关
     */
    @PostMapping("/red-led/{clientId}")
    public Result<String> redLed(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid LedRequest request) {
        String cmdId = camService.controlRedLed(clientId, request.getValue());
        return Result.success("红色指示灯指令已发送", cmdId);
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
    public Result<DeviceStatusResponse> getStatus(@PathVariable @NotBlank String clientId) {
        DeviceStatusResponse status = camService.getDeviceStatus(clientId);
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
     * 刷新设备配置（POST方式，供前端刷新按钮使用）
     */
    @PostMapping("/cam/{clientId}/get_config")
    public Result<String> refreshConfig(@PathVariable @NotBlank String clientId) {
        String cmdId = camService.getConfig(clientId);
        return Result.success("配置刷新指令已发送", cmdId);
    }


    /**
     * 图片上传接口 (ESP32调用)
     */
    @PostMapping(value = "/cam/upload", consumes = "multipart/form-data")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName) {
        try {
            // 创建图片保存目录
            Path photosDirPath = Paths.get(photosDir);
            Files.createDirectories(photosDirPath);
            
            // 保存文件
            Path filePath = photosDirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            long fileSize = Files.size(filePath);
            log.info("图片上传成功: {}, 大小: {} bytes", fileName, fileSize);
            
            // 从文件名解析clientId和cmdId（格式: clientId_cmdId.jpg）
            String clientId = "unknown";
            String cmdId = "0";
            if (fileName.contains("_") && fileName.contains(".")) {
                String baseName = fileName.substring(0, fileName.lastIndexOf("."));
                String[] parts = baseName.split("_");
                if (parts.length >= 2) {
                    clientId = parts[0];
                    cmdId = parts[1];
                }
            }
            
            // 通过SSE推送拍照结果
            sseService.pushCaptureResult(clientId, cmdId, fileName);
            
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
     * 设置DHT读取间隔
     */
    @PostMapping("/dht-interval/{clientId}")
    public Result<String> setDhtInterval(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid DhtIntervalRequest request) {
        String cmdId = camService.setDhtInterval(clientId, request.getInterval());
        return Result.success("DHT间隔设置指令已发送", cmdId);
    }
    
    /**
     * 设置状态上报间隔
     */
    @PostMapping("/cam/{clientId}/set_status_interval")
    public Result<String> setStatusInterval(
            @PathVariable @NotBlank String clientId,
            @RequestBody Map<String, Integer> request) {
        Integer interval = request.get("interval");
        if (interval == null || interval < 1000) {
            return Result.error("间隔不能小于1秒");
        }
        String cmdId = camService.setStatusInterval(clientId, interval);
        return Result.success("状态上报间隔设置指令已发送", cmdId);
    }
    
    /**
     * 控制舵机角度 (窗户控制)
     */
    @PostMapping("/servo/{clientId}")
    public Result<String> setServo(
            @PathVariable @NotBlank String clientId,
            @RequestBody @Valid ServoRequest request) {
        String cmdId = camService.controlServo(clientId, request.getAngle());
        String status = request.getAngle() == 0 ? "关闭" : 
                       request.getAngle() == 45 ? "小开" :
                       request.getAngle() == 90 ? "半开" :
                       request.getAngle() == 180 ? "全开" : request.getAngle() + "°";
        return Result.success("窗户控制指令已发送: " + status, cmdId);
    }
    
    /**
     * 控制继电器 (风扇控制)
     */
    @PostMapping("/relay/{clientId}")
    public Result<String> setRelay(
            @PathVariable @NotBlank String clientId,
            @RequestBody Map<String, Boolean> request) {
        Boolean on = request.get("on");
        if (on == null) {
            return Result.error("参数on不能为空");
        }
        String cmdId = camService.controlRelay(clientId, on);
        return Result.success("风扇" + (on ? "开启" : "关闭") + "指令已发送", cmdId);
    }
}
