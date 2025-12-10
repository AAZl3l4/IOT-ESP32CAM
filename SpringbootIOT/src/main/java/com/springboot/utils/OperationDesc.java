package com.springboot.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作描述工具类 - 将操作类型转换为中文描述
 */
public class OperationDesc {
    
    private static final Map<String, String> OPERATION_MAP = new HashMap<>();
    
    static {
        OPERATION_MAP.put("capture", "拍照上传");
        OPERATION_MAP.put("led", "LED开关控制");
        OPERATION_MAP.put("led_brightness", "LED亮度调节");
        OPERATION_MAP.put("red_led", "红色指示灯控制");
        
        // 分辨率
        OPERATION_MAP.put("framesize", "分辨率设置");
        
        // 摄像头参数
        OPERATION_MAP.put("brightness", "亮度调整");
        OPERATION_MAP.put("contrast", "对比度调整");
        OPERATION_MAP.put("saturation", "饱和度调整");
        OPERATION_MAP.put("quality", "JPEG质量设置");
        OPERATION_MAP.put("special_effect", "特效设置");
        OPERATION_MAP.put("wb_mode", "白平衡模式");
        OPERATION_MAP.put("ae_level", "自动曝光级别");
        
        // 配置管理
        OPERATION_MAP.put("set_wifi", "WiFi配置");
        OPERATION_MAP.put("set_mqtt", "MQTT配置");
        OPERATION_MAP.put("set_upload_url", "上传URL配置");
        OPERATION_MAP.put("reset_config", "重置配置");
        OPERATION_MAP.put("get_config", "查询配置");
        OPERATION_MAP.put("set_dht_interval", "DHT读取间隔设置");
        OPERATION_MAP.put("set_status_interval", "状态上报间隔设置");
    }
    
    /**
     * 获取操作的中文描述
     */
    public static String getDesc(String operation) {
        return OPERATION_MAP.getOrDefault(operation, "未知操作");
    }
    
    /**
     * 构建完整的操作描述(带参数)
     */
    public static String getFullDesc(String operation, Integer value) {
        String baseDesc = getDesc(operation);
        
        if (value == null) {
            return baseDesc;
        }
        
        switch (operation) {
            case "capture":
                return "拍照上传(1080p)";
            case "led":
                return value == 1 ? "LED开启" : "LED关闭";
            case "red_led":
                return value == 1 ? "指示灯开启" : "指示灯关闭";
            case "led_brightness":
                return String.format("LED亮度设为%d", value);
            case "framesize":
                return getFramesizeDesc(value);
            case "special_effect":
                return getEffectDesc(value);
            default:
                return String.format("%s(值:%d)", baseDesc, value);
        }
    }
    
    /**
     * 分辨率描述
     */
    private static String getFramesizeDesc(int framesize) {
        switch (framesize) {
            case 7: return "分辨率设为480p";
            case 11: return "分辨率设为720p";  
            case 14: return "分辨率设为1080p";
            default: return "分辨率设为" + framesize;
        }
    }
    
    /**
     * 特效描述
     */
    private static String getEffectDesc(int effect) {
        switch (effect) {
 case 0: return "特效:无";
            case 1: return "特效:负片";
            case 2: return "特效:黑白";
            case 3: return "特效:复古";
            default: return "特效:" + effect;
        }
    }
}
