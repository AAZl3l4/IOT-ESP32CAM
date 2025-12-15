/**
 * @file status_publisher.cpp
 * @brief 状态发布模块
 * 
 * 负责：
 * - 发布指令执行结果
 * - 发布设备状态（精简版）
 * - 发布设备完整配置
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
unsigned long lastStatusReport = 0;
unsigned long statusReportInterval = 60000;  // 状态上报间隔(毫秒)，默认60秒

/**
 * 发布指令执行结果
 */
void publishResult(long cmdId, bool ok, const char* info) {
  StaticJsonDocument<256> doc;
  doc["id"] = cmdId;
  doc["ok"] = ok;
  doc["info"] = info;
  
  char buffer[256];
  serializeJson(doc, buffer);
  
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/result", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 1);
  Serial.printf("已发布结果: %s\n", buffer);
}

/**
 * 发布设备状态（精简版，仅包含需要实时监控的数据）
 * 完整配置信息通过 publishConfig() 发送
 */
void publishStatus() {
  StaticJsonDocument<256> doc;
  doc["clientId"] = mqtt_client_id;
  doc["uptime"] = millis() / 1000;
  doc["freeHeap"] = ESP.getFreeHeap();
  doc["rssi"] = WiFi.RSSI();
  doc["ledStatus"] = ledStatus;
  doc["ledBrightness"] = ledBrightness;  // LED亮度实时回显
  doc["redLedStatus"] = redLedStatus;
  doc["servoAngle"] = servoAngle;  // 舵机角度实时回显
  
  sensor_t *s = esp_camera_sensor_get();
  doc["framesize"] = s->status.framesize;
  
  char buffer[256];
  serializeJson(doc, buffer);
  
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/status", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 0);
  Serial.printf("已发布状态: %s\n", buffer);
}

/**
 * 发布设备完整配置
 * @param cmdId 命令ID，如果为0则仅发布配置，否则同时发布result
 */
void publishConfig(long cmdId) {
  // 使用更大的JSON文档容量以容纳所有配置
  StaticJsonDocument<2048> doc;
  
  // 基础信息
  doc["clientId"] = mqtt_client_id;
  doc["uptime"] = millis() / 1000;
  doc["freeHeap"] = ESP.getFreeHeap();
  
  // WiFi配置
  doc["wifiSsid"] = wifi_ssid;
  doc["wifiPassword"] = wifi_password;
  doc["wifiIp"] = WiFi.localIP().toString();
  doc["rssi"] = WiFi.RSSI();
  
  // MQTT配置
  doc["mqttBroker"] = mqtt_server;
  doc["mqttPort"] = mqtt_port;
  
  // LED状态
  doc["ledStatus"] = ledStatus;
  doc["ledBrightness"] = ledBrightness;
  doc["redLedStatus"] = redLedStatus;
  
  // DHT和状态上报间隔
  doc["dhtInterval"] = dhtReadInterval;
  doc["statusInterval"] = statusReportInterval;
  
  // 上传URL
  doc["uploadUrl"] = upload_url;
  
  // 摄像头配置
  sensor_t *s = esp_camera_sensor_get();
  if (s != NULL) {
    doc["framesize"] = s->status.framesize;
    doc["quality"] = s->status.quality;
    doc["brightness"] = s->status.brightness;
    doc["contrast"] = s->status.contrast;
    doc["saturation"] = s->status.saturation;
    doc["specialEffect"] = s->status.special_effect;
    doc["whiteBalance"] = s->status.awb;
    doc["awbGain"] = s->status.awb_gain;
    doc["wbMode"] = s->status.wb_mode;
    doc["exposureCtrl"] = s->status.aec;
    doc["aec"] = s->status.aec;
    doc["aecValue"] = s->status.aec_value;
    doc["aec2"] = s->status.aec2;
    doc["gainCtrl"] = s->status.agc;
    doc["agcGain"] = s->status.agc_gain;
    doc["gainceiling"] = s->status.gainceiling;
    doc["bpc"] = s->status.bpc;
    doc["wpc"] = s->status.wpc;
    doc["rawGma"] = s->status.raw_gma;
    doc["lenc"] = s->status.lenc;
    doc["hmirror"] = s->status.hmirror;
    doc["vflip"] = s->status.vflip;
    doc["dcw"] = s->status.dcw;
    doc["colorbar"] = s->status.colorbar;
  }
  
  char buffer[2048];
  serializeJson(doc, buffer);
  
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/config", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 0);
  Serial.printf("已发布配置: %d字节\n", strlen(buffer));
  
  // 如果有cmdId，同时发布result
  if (cmdId > 0) {
    publishResult(cmdId, true, "配置已发送");
  }
}
