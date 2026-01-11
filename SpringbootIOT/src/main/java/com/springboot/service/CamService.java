package com.springboot.service;

import com.springboot.pojo.vo.DeviceStatusResponse;

public interface CamService {
    /**
     * 触发拍照指令
     * @param clientId ESP 的 clientId
     * @return 命令序号
     */
    String triggerCapture(String clientId);

    /**
     * 控制LED开关
     * @param clientId ESP 的 clientId
     * @param value 0=关闭, 1=开启
     * @return 命令序号
     */
    String controlLed(String clientId, int value);

    /**
     * 设置LED亮度
     * @param clientId ESP 的 clientId
     * @param value 亮度值(0-255)
     * @return 命令序号
     */
    String setLedBrightness(String clientId, int value);

    /**
     * 控制红色指示灯开关
     * @param clientId ESP 的 clientId
     * @param value 0=关闭, 1=开启
     * @return 命令序号
     */
    String controlRedLed(String clientId, int value);

    /**
     * 设置摄像头参数
     * @param clientId ESP 的 clientId
     * @param param 参数名称(brightness, contrast, saturation等)
     * @param value 参数值
     * @return 命令序号
     */
    String setCameraParam(String clientId, String param, int value);

    /**
     * 获取设备状态
     * @param clientId ESP 的 clientId
     * @return 设备状态信息
     */
    DeviceStatusResponse getDeviceStatus(String clientId);

    /**
     * 设置视频流分辨率
     * @param clientId ESP 的 clientId
     * @param framesize 分辨率代码 (7=480p, 11=720p, 14=1080p)
     * @return 命令序号
     */
    String setStreamResolution(String clientId, int framesize);

    /**
     * 设置WiFi配置
     * @param clientId ESP 的 clientId
     * @param ssid WiFi名称
     * @param password WiFi密码
     * @return 命令序号
     */
    String setWiFiConfig(String clientId, String ssid, String password);

    /**
     * 设置MQTT配置
     * @param clientId ESP 的 clientId
     * @param server MQTT服务器地址
     * @param port MQTT端口
     * @param mqttClientId MQTT客户端ID
     * @return 命令序号
     */
    String setMQTTConfig(String clientId, String server, int port, String mqttClientId);

    /**
     * 设置上传URL
     * @param clientId ESP 的 clientId
     * @param url 上传地址
     * @return 命令序号
     */
    String setUploadUrl(String clientId, String url);

    /**
     * 重置为默认配置
     * @param clientId ESP 的 clientId
     * @return 命令序号
     */
    String resetConfig(String clientId);

    /**
     * 查询设备配置
     * @param clientId ESP 的 clientId
     * @return 命令序号
     */
    String getConfig(String clientId);

    /**
     * 设置DHT读取间隔
     * @param clientId ESP 的 clientId
     * @param interval 间隔(毫秒), 1000-60000
     * @return 命令序号
     */
    String setDhtInterval(String clientId, int interval);
    
    /**
     * 设置状态上报间隔
     * @param clientId ESP 的 clientId
     * @param interval 间隔(毫秒), 1000-300000
     * @return 命令序号
     */
    String setStatusInterval(String clientId, int interval);
    
    /**
     * 控制舵机角度 (窗户控制)
     * @param clientId ESP 的 clientId
     * @param angle 角度(0-180), 0=关闭, 45=小开, 90=半开, 180=全开
     * @return 命令序号
     */
    String controlServo(String clientId, int angle);
    
    /**
     * 控制继电器 (风扇控制)
     * @param clientId ESP 的 clientId
     * @param on true=开启, false=关闭
     * @return 命令序号
     */
    String controlRelay(String clientId, boolean on);
    
    /**
     * 触发拍照并返回用于等待结果的Future
     * @param clientId ESP 的 clientId
     * @return CaptureResult包含cmdId和用于等待的Future
     */
    CaptureResult triggerCaptureWithWait(String clientId);
    
    /**
     * 当图片上传完成时调用，通知等待的Future
     * @param cmdId 命令ID
     * @param fileName 上传的文件名
     */
    void notifyCaptureComplete(String cmdId, String fileName);
    
    /**
     * 拍照结果包装类
     */
    record CaptureResult(long cmdId, java.util.concurrent.CompletableFuture<String> future) {}
}
