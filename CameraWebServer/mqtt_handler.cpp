/**
 * @file mqtt_handler.cpp
 * @brief MQTT通信处理模块
 * 
 * 负责：
 * - MQTT连接和重连
 * - 消息接收和回调处理
 * - 指令解析和分发
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
WiFiClient espClient;
PubSubClient mqttClient(espClient);

/**
 * 初始化MQTT连接
 */
void setupMQTT() {
  mqttClient.setServer(mqtt_server.c_str(), mqtt_port);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(2560);  // 需要足够大以发送完整配置(2048+)
  reconnectMQTT();
}

/**
 * MQTT重连
 */
void reconnectMQTT() {
  int retry = 0;
  while (!mqttClient.connected() && retry < 5) {
    Serial.print("正在连接MQTT...");
    
    if (mqttClient.connect(mqtt_client_id.c_str())) {
      Serial.println("连接成功");
      
      // 订阅指令topic
      char cmdTopic[64];
      snprintf(cmdTopic, sizeof(cmdTopic), "cam/%s/cmd", mqtt_client_id.c_str());
      mqttClient.subscribe(cmdTopic, 1);
      Serial.printf("已订阅: %s\n", cmdTopic);
      
      // 发送上线消息
      publishStatus();
    } else {
      Serial.print("连接失败, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" 5秒后重试");
      delay(5000);
      retry++;
    }
  }
}

/**
 * MQTT消息回调函数
 */
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.printf("MQTT消息 [%s]: ", topic);
  
  for (unsigned int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();

  // 解析JSON
  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, payload, length);
  
  if (error) {
    Serial.print("JSON解析失败: ");
    Serial.println(error.c_str());
    return;
  }

  // 处理命令
  handleCommand(doc);
}

/**
 * 处理MQTT指令
 */
void handleCommand(StaticJsonDocument<512>& doc) {
  if (!doc.containsKey("id") || !doc.containsKey("op")) {
    Serial.println("无效的指令格式");
    return;
  }

  // 使用 as<long>() 显式转换确保正确获取时间戳
  long cmdId = doc["id"].as<long>();
  const char* op = doc["op"];

  Serial.printf("处理指令: id=%ld, op=%s\n", cmdId, op);

  // ===== 配置管理指令 =====
  if (strcmp(op, "set_wifi") == 0) {
    // 设置WiFi配置
    if (doc.containsKey("ssid") && doc.containsKey("password")) {
      String new_ssid = doc["ssid"].as<String>();
      String new_pass = doc["password"].as<String>();
      
      saveWiFiConfig(new_ssid, new_pass);
      publishResult(cmdId, true, "WiFi配置已保存，设备重启中...");
      delay(1000);
      ESP.restart();
    } else {
      publishResult(cmdId, false, "缺少SSID或密码");
    }
    return;
  }
  
  else if (strcmp(op, "set_mqtt") == 0) {
    // 设置MQTT配置
    if (doc.containsKey("server")) {
      String new_server = doc["server"].as<String>();
      int new_port = doc["port"] | mqtt_port;
      String new_client = doc.containsKey("clientId") ? doc["clientId"].as<String>() : mqtt_client_id;
      
      saveMQTTConfig(new_server, new_port, new_client);
      publishResult(cmdId, true, "MQTT配置已保存，设备重启中...");
      delay(1000);
      ESP.restart();
    } else {
      publishResult(cmdId, false, "缺少MQTT服务器地址");
    }
    return;
  }
  
  else if (strcmp(op, "set_upload_url") == 0) {
    // 设置上传URL
    if (doc.containsKey("url")) {
      String new_url = doc["url"].as<String>();
      saveUploadUrl(new_url);
      upload_url = new_url;  // 立即生效
      publishResult(cmdId, true, "上传URL已更新");
    } else {
      publishResult(cmdId, false, "缺少URL参数");
    }
    return;
  }
  
  else if (strcmp(op, "reset_config") == 0) {
    // 恢复默认配置
    resetConfig();
    publishResult(cmdId, true, "配置已重置，设备重启中...");
    delay(1000);
    ESP.restart();
    return;
  }
  
  else if (strcmp(op, "get_config") == 0) {
    // 查询配置
    publishConfig(cmdId);
    return;
  }
  
  else if (strcmp(op, "set_dht_interval") == 0) {
    // 设置DHT读取间隔
    int interval = doc["val"] | 5000;
    if (interval < 1000) interval = 1000;
    if (interval > 60000) interval = 60000;
    dhtReadInterval = interval;
    char info[48];
    snprintf(info, sizeof(info), "DHT读取间隔设为%d毫秒", interval);
    publishResult(cmdId, true, info);
    Serial.printf("DHT读取间隔已设置为 %d 毫秒\n", interval);
    return;
  }
  
  else if (strcmp(op, "set_status_interval") == 0) {
    // 设置状态上报间隔
    int interval = doc["val"] | 60000;
    if (interval < 10000) interval = 10000;
    if (interval > 300000) interval = 300000;
    statusReportInterval = interval;
    char info[48];
    snprintf(info, sizeof(info), "状态上报间隔设为%d毫秒", interval);
    publishResult(cmdId, true, info);
    Serial.printf("状态上报间隔已设置为 %d 毫秒\n", interval);
    return;
  }

  // ===== 摄像头控制指令 =====
  int val = doc["val"] | 0;
  
  if (strcmp(op, "capture") == 0) {
    captureAndUpload(cmdId);
  } else if (strcmp(op, "led") == 0) {
    controlLED(val);
    publishResult(cmdId, true, val ? "LED开启" : "LED关闭");
  } else if (strcmp(op, "led_brightness") == 0) {
    setLEDBrightness(val);
    char info[32];
    snprintf(info, sizeof(info), "亮度设置为%d", val);
    publishResult(cmdId, true, info);
  } else if (strcmp(op, "red_led") == 0) {
    controlRedLED(val);
    publishResult(cmdId, true, val ? "指示灯开启" : "指示灯关闭");
  } else if (strcmp(op, "framesize") == 0) {
    setCameraParam("framesize", val);
    publishResult(cmdId, true, "分辨率已更新");
  } 
  // ===== 舵机控制指令 (窗户) =====
  else if (strcmp(op, "servo") == 0) {
    setServoAngle(val);
    char info[48];
    snprintf(info, sizeof(info), "窗户%s (角度:%d°)", getWindowStatus(val), val);
    publishResult(cmdId, true, info);
  } else if (strcmp(op, "servo_full") == 0) {
    setServoAngle(180);
    publishResult(cmdId, true, "窗户全开 (180°)");
  } else if (strcmp(op, "servo_half") == 0) {
    setServoAngle(90);
    publishResult(cmdId, true, "窗户半开 (90°)");
  } else if (strcmp(op, "servo_small") == 0) {
    setServoAngle(45);
    publishResult(cmdId, true, "窗户小开 (45°)");
  } else if (strcmp(op, "servo_close") == 0) {
    setServoAngle(0);
    publishResult(cmdId, true, "窗户关闭 (0°)");
  } 
  // ===== 继电器控制指令 (风扇) =====
  else if (strcmp(op, "relay") == 0 || strcmp(op, "fan") == 0) {
    bool on = (val != 0);
    controlRelay(on);
    publishResult(cmdId, true, on ? "风扇开启" : "风扇关闭");
  } else if (strcmp(op, "fan_on") == 0) {
    controlRelay(true);
    publishResult(cmdId, true, "风扇开启");
  } else if (strcmp(op, "fan_off") == 0) {
    controlRelay(false);
    publishResult(cmdId, true, "风扇关闭");
  } else {
    setCameraParam(op, val);
    char info[64];
    snprintf(info, sizeof(info), "参数%s设置为%d", op, val);
    publishResult(cmdId, true, info);
  }
}
