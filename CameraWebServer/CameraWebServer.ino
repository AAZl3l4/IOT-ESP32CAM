#include "esp_camera.h"
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <Preferences.h>

// ===========================
// Select camera model in board_config.h
// ===========================
#include "board_config.h"

// ===========================
// 默认配置（首次启动使用）
// ===========================
#define DEFAULT_WIFI_SSID "2702"
#define DEFAULT_WIFI_PASS "18063328637"
#define DEFAULT_MQTT_SERVER "broker.emqx.io"
#define DEFAULT_MQTT_PORT 1883
#define DEFAULT_MQTT_CLIENT "esp32cam"
#define DEFAULT_UPLOAD_URL "http://192.168.124.68:8080/mqtt/cam/upload"

// 配置变量（从Flash读取或使用默认值）
String wifi_ssid;
String wifi_password;
String mqtt_server;
int mqtt_port;
String mqtt_client_id;
String upload_url;

// Preferences对象
Preferences preferences;

// ===========================
// 全局对象
// ===========================
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// ===========================
// LED控制相关
// ===========================
#define LED_PIN 4
#define LED_PWM_CHANNEL 15  // 避免与摄像头冲突
#define LED_PWM_FREQ 5000
#define LED_PWM_RESOLUTION 8  // 8位分辨率(0-255)

bool ledStatus = false;
int ledBrightness = 128;

// ===========================
// 函数声明
// ===========================
void startCameraServer();
void setupLedFlash();
void loadConfig();
void saveWiFiConfig(const String& ssid, const String& pass);
void saveMQTTConfig(const String& server, int port, const String& clientId);
void saveUploadUrl(const String& url);
void resetConfig();
void publishConfig(long cmdId);
void setupMQTT();
void reconnectMQTT();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void handleCommand(StaticJsonDocument<512>& doc);
void captureAndUpload(long cmdId);
void uploadImage(camera_fb_t *fb, long cmdId);
void publishResult(long cmdId, bool ok, const char* info);
void publishStatus();
void controlLED(int value);
void setLEDBrightness(int value);
void setCameraParam(const char* param, int value);

// ===========================
// Setup函数
// ===========================
void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println();
  Serial.println("ESP32-CAM MQTT 物联网设备启动中...");

  // ===========================
  // 加载配置
  // ===========================
  loadConfig();
  
  Serial.println("配置已加载:");
  Serial.printf("WiFi名称: %s\n", wifi_ssid.c_str());
  Serial.printf("MQTT服务器: %s:%d\n", mqtt_server.c_str(), mqtt_port);
  Serial.printf("MQTT客户端ID: %s\n", mqtt_client_id.c_str());
  Serial.printf("上传地址: %s\n", upload_url.c_str());

  // ===========================
  // 初始化摄像头
  // ===========================
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.frame_size = FRAMESIZE_HD;  // 默认720p用于推流
  config.pixel_format = PIXFORMAT_JPEG;
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count = 1;

  // 如果有PSRAM，提高配置
  if (config.pixel_format == PIXFORMAT_JPEG) {
    if (psramFound()) {
      config.jpeg_quality = 10;
      config.fb_count = 1;  // 改为1避免重影
      config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;  // 使用WHEN_EMPTY避免旧帧
    } else {
      config.frame_size = FRAMESIZE_SVGA;
      config.fb_location = CAMERA_FB_IN_DRAM;
    }
  } else {
    config.frame_size = FRAMESIZE_240X240;
#if CONFIG_IDF_TARGET_ESP32S3
    config.fb_count = 2;
#endif
  }

#if defined(CAMERA_MODEL_ESP_EYE)
  pinMode(13, INPUT_PULLUP);
  pinMode(14, INPUT_PULLUP);
#endif

  // 初始化摄像头
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("摄像头初始化失败，错误码: 0x%x\n", err);
    return;
  }
  Serial.println("摄像头初始化成功");

  sensor_t *s = esp_camera_sensor_get();
  // 调整摄像头参数
  if (s->id.PID == OV3660_PID) {
    s->set_vflip(s, 1);
    s->set_brightness(s, 1);
    s->set_saturation(s, -2);
  }

#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

#if defined(CAMERA_MODEL_ESP32S3_EYE)
  s->set_vflip(s, 1);
#endif

  // ===========================
  // 初始化LED（PWM模式）
  // ===========================
#if defined(LED_GPIO_NUM)
  ledcAttach(LED_PIN, LED_PWM_FREQ, LED_PWM_RESOLUTION);
  ledcWrite(LED_PIN, 0);  // 初始关闭
  Serial.println("LED PWM初始化完成");
#endif

  // ===========================
  // 连接WiFi
  // ===========================
  WiFi.begin(wifi_ssid.c_str(), wifi_password.c_str());
  WiFi.setSleep(false);

  Serial.print("正在连接WiFi: ");
  Serial.println(wifi_ssid);
  int wifi_retry = 0;
  while (WiFi.status() != WL_CONNECTED && wifi_retry < 30) {
    delay(500);
    Serial.print(".");
    wifi_retry++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("");
    Serial.println("WiFi连接成功");
    Serial.print("IP地址: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nWiFi连接失败！");
    return;
  }

  // ===========================
  // 启动Web视频流服务器（仅保留流推送功能）
  // ===========================
  startCameraServer();
  Serial.print("视频流地址: http://");
  Serial.print(WiFi.localIP());
  Serial.println("/stream");

  // ===========================
  // 初始化MQTT
  // ===========================
  setupMQTT();

  Serial.println("\n=================================");
  Serial.println("ESP32-CAM 就绪！");
  Serial.printf("客户端ID: %s\n", mqtt_client_id.c_str());
  Serial.println("=================================\n");
}

// ===========================
// Loop函数
// ===========================
void loop() {
  // 维持MQTT连接
  if (!mqttClient.connected()) {
    reconnectMQTT();
  }
  mqttClient.loop();

  // 每30秒上报一次设备状态
  static unsigned long lastStatusReport = 0;
  if (millis() - lastStatusReport > 30000) {
    publishStatus();
    lastStatusReport = millis();
  }

  delay(10);
}

// ===========================
// 配置管理函数
// ===========================

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

/**
 * 发布当前配置
 */
void publishConfig(long cmdId) {
  StaticJsonDocument<512> doc;
  doc["id"] = cmdId;
  doc["ok"] = true;
  
  JsonObject config = doc.createNestedObject("config");
  config["wifi_ssid"] = wifi_ssid;
  config["mqtt_server"] = mqtt_server;
  config["mqtt_port"] = mqtt_port;
  config["mqtt_client"] = mqtt_client_id;
  config["upload_url"] = upload_url;
  
  char buffer[512];
  serializeJson(doc, buffer);
  
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/result", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 1);
  Serial.printf("已发布配置: %s\n", buffer);
}

// ===========================
// MQTT相关函数
// ===========================

/**
 * 初始化MQTT连接
 */
void setupMQTT() {
  mqttClient.setServer(mqtt_server.c_str(), mqtt_port);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(512);
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
      ESP.restart();  // 重启应用新WiFi配置
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
      ESP.restart();  // 重启应用新MQTT配置
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
  } else if (strcmp(op, "framesize") == 0) {
    setCameraParam("framesize", val);
    publishResult(cmdId, true, "分辨率已更新");
  } else {
    setCameraParam(op, val);
    char info[64];
    snprintf(info, sizeof(info), "参数%s设置为%d", op, val);
    publishResult(cmdId, true, info);
  }
}

/**
 * 拍照并上传
 */
void captureAndUpload(long cmdId) {
  Serial.println("开始拍照上传...");
  Serial.printf("收到指令ID: %ld\n", cmdId);
  
  sensor_t *s = esp_camera_sensor_get();
  framesize_t old_framesize = s->status.framesize;
  
  if (old_framesize != FRAMESIZE_FHD) {
    s->set_framesize(s, FRAMESIZE_FHD);
    delay(100);
  }
  
  camera_fb_t *fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("摄像头拍照失败");
    publishResult(cmdId, false, "拍照失败");
    if (old_framesize != FRAMESIZE_FHD) {
      s->set_framesize(s, old_framesize);
    }
    return;
  }

  Serial.printf("已拍照片: %d 字节\n", fb->len);
  uploadImage(fb, cmdId);
  esp_camera_fb_return(fb);
  
  if (old_framesize != FRAMESIZE_FHD) {
    s->set_framesize(s, old_framesize);
  }
}

/**
 * HTTP上传图片到后端
 */
void uploadImage(camera_fb_t *fb, long cmdId) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi未连接，上传失败");
    publishResult(cmdId, false, "WiFi未连接");
    return;
  }

  Serial.println("开始上传图片...");
  
  char fileName[64];
  snprintf(fileName, sizeof(fileName), "%s_%ld.jpg", mqtt_client_id.c_str(), cmdId);
  Serial.printf("文件名: %s (cmdId: %ld)\n", fileName, cmdId);
  Serial.printf("图片大小: %d 字节\n", fb->len);
  
  WiFiClient httpClient;
  HTTPClient http;
  
  if (!http.begin(httpClient, upload_url.c_str())) {
    Serial.println("✗ HTTP初始化失败");
    publishResult(cmdId, false, "HTTP初始化失败");
    http.end();
    return;
  }
  
  http.setTimeout(20000);
  
  String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
  String contentType = "multipart/form-data; boundary=" + boundary;
  
  String body = "";
  body += "--" + boundary + "\r\n";
  body += "Content-Disposition: form-data; name=\"fileName\"\r\n\r\n";
  body += String(fileName) + "\r\n";
  body += "--" + boundary + "\r\n";
  body += "Content-Disposition: form-data; name=\"file\"; filename=\"" + String(fileName) + "\"\r\n";
  body += "Content-Type: image/jpeg\r\n\r\n";
  
  String endBoundary = "\r\n--" + boundary + "--\r\n";
  
  size_t totalLen = body.length() + fb->len + endBoundary.length();
  Serial.printf("总上传大小: %d 字节\n", totalLen);
  
  uint8_t* postData = (uint8_t*)malloc(totalLen);
  if (postData == NULL) {
    Serial.println("✗ 内存分配失败");
    publishResult(cmdId, false, "内存不足");
    http.end();
    return;
  }
  
  size_t offset = 0;
  memcpy(postData + offset, body.c_str(), body.length());
  offset += body.length();
  memcpy(postData + offset, fb->buf, fb->len);
  offset += fb->len;
  memcpy(postData + offset, endBoundary.c_str(), endBoundary.length());
  
  http.addHeader("Content-Type", contentType);
  http.addHeader("Content-Length", String(totalLen));
  
  Serial.println("发送POST请求...");
  
  int httpCode = http.POST(postData, totalLen);
  free(postData);
  
  if (httpCode > 0) {
    Serial.printf("HTTP响应码: %d\n", httpCode);
    String response = http.getString();
    Serial.println("响应: " + response);
    
    if (httpCode == 200) {
      Serial.println("✓ 上传成功！");
      publishResult(cmdId, true, "上传成功");
    } else {
      Serial.printf("✗ 上传失败，错误码 %d\n", httpCode);
      publishResult(cmdId, false, "上传失败");
    }
  } else {
    Serial.printf("✗ HTTP错误: %s\n", http.errorToString(httpCode).c_str());
    publishResult(cmdId, false, "HTTP错误");
  }
  
  http.end();
  Serial.println("上传完成");
}

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
 * 发布设备状态
 */
void publishStatus() {
  StaticJsonDocument<384> doc;
  doc["clientId"] = mqtt_client_id;
  doc["uptime"] = millis() / 1000;
  doc["freeHeap"] = ESP.getFreeHeap();
  doc["rssi"] = WiFi.RSSI();
  doc["ledStatus"] = ledStatus;
  doc["ledBrightness"] = ledBrightness;
  
  sensor_t *s = esp_camera_sensor_get();
  doc["framesize"] = s->status.framesize;
  
  char buffer[384];
  serializeJson(doc, buffer);
  
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/status", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 0);
  Serial.printf("已发布状态: %s\n", buffer);
}

/**
 * LED开关控制
 */
void controlLED(int value) {
  ledStatus = (value != 0);
  if (ledStatus) {
    ledcWrite(LED_PIN, ledBrightness);
  } else {
    ledcWrite(LED_PIN, 0);
  }
  Serial.printf("LED %s\n", ledStatus ? "开启" : "关闭");
}

/**
 * LED亮度调节
 */
void setLEDBrightness(int value) {
  if (value < 0) value = 0;
  if (value > 255) value = 255;
  
  ledBrightness = value;
  if (ledStatus) {
    ledcWrite(LED_PIN, ledBrightness);
  }
  Serial.printf("LED亮度设置为 %d\n", ledBrightness);
}

/**
 * 设置摄像头参数
 */
void setCameraParam(const char* param, int value) {
  sensor_t *s = esp_camera_sensor_get();
  
  if (strcmp(param, "framesize") == 0) {
    s->set_framesize(s, (framesize_t)value);
  } else if (strcmp(param, "quality") == 0) {
    s->set_quality(s, value);
  } else if (strcmp(param, "brightness") == 0) {
    s->set_brightness(s, value);
  } else if (strcmp(param, "contrast") == 0) {
    s->set_contrast(s, value);
  } else if (strcmp(param, "saturation") == 0) {
    s->set_saturation(s, value);
  } else if (strcmp(param, "sharpness") == 0) {
    s->set_sharpness(s, value);
  } else if (strcmp(param, "denoise") == 0) {
    s->set_denoise(s, value);
  } else if (strcmp(param, "special_effect") == 0) {
    s->set_special_effect(s, value);
  } else if (strcmp(param, "wb_mode") == 0) {
    s->set_wb_mode(s, value);
  } else if (strcmp(param, "ae_level") == 0) {
    s->set_ae_level(s, value);
  } else if (strcmp(param, "aec_value") == 0) {
    s->set_aec_value(s, value);
  } else if (strcmp(param, "gainceiling") == 0) {
    s->set_gainceiling(s, (gainceiling_t)value);
  }
  
  Serial.printf("摄像头参数 %s 设置为 %d\n", param, value);
}