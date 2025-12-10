/**
 * @file config_manager.cpp
 * @brief 配置管理模块
 * 
 * 负责：
 * - 从Flash加载配置（Preferences库）
 * - 保存WiFi、MQTT、上传URL配置
 * - 恢复默认配置
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
String wifi_ssid;
String wifi_password;
String mqtt_server;
int mqtt_port;
String mqtt_client_id;
String upload_url;

Preferences preferences;

/**
 * 从Flash加载配置
 */
void loadConfig() {
  preferences.begin("esp32cam", true);  // 只读模式
  
  // 读取WiFi配置
  wifi_ssid = preferences.getString("wifi_ssid", DEFAULT_WIFI_SSID);
  wifi_password = preferences.getString("wifi_pass", DEFAULT_WIFI_PASS);
  
  // 读取MQTT配置
  mqtt_server = preferences.getString("mqtt_server", DEFAULT_MQTT_SERVER);
  mqtt_port = preferences.getInt("mqtt_port", DEFAULT_MQTT_PORT);
  mqtt_client_id = preferences.getString("mqtt_client", DEFAULT_MQTT_CLIENT);
  
  // 读取上传URL
  upload_url = preferences.getString("upload_url", DEFAULT_UPLOAD_URL);
  
  preferences.end();
}

/**
 * 保存WiFi配置
 */
void saveWiFiConfig(const String& ssid, const String& pass) {
  preferences.begin("esp32cam", false);
  preferences.putString("wifi_ssid", ssid);
  preferences.putString("wifi_pass", pass);
  preferences.end();
  
  Serial.println("WiFi配置已保存到Flash");
}

/**
 * 保存MQTT配置
 */
void saveMQTTConfig(const String& server, int port, const String& clientId) {
  preferences.begin("esp32cam", false);
  preferences.putString("mqtt_server", server);
  preferences.putInt("mqtt_port", port);
  preferences.putString("mqtt_client", clientId);
  preferences.end();
  
  Serial.println("MQTT配置已保存到Flash");
}

/**
 * 保存上传URL
 */
void saveUploadUrl(const String& url) {
  preferences.begin("esp32cam", false);
  preferences.putString("upload_url", url);
  preferences.end();
  
  Serial.println("上传URL已保存到Flash");
}

/**
 * 重置为默认配置
 */
void resetConfig() {
  preferences.begin("esp32cam", false);
  preferences.clear();
  preferences.end();
  
  Serial.println("配置已重置为默认值");
}
