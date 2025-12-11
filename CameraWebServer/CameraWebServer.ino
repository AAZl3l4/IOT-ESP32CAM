/**
 * @file CameraWebServer.ino
 * @brief ESP32-CAM MQTT物联网设备 - 主程序入口
 * 
 * 本项目已拆分为以下模块：
 * - config.h           - 全局配置和声明
 * - config_manager.cpp - 配置管理（Flash读写）
 * - mqtt_handler.cpp   - MQTT通信处理
 * - camera_control.cpp - 摄像头拍照和上传
 * - led_control.cpp    - LED和指示灯控制
 * - dht_sensor.cpp     - DHT22温湿度传感器
 * - status_publisher.cpp - 状态发布
 * - app_httpd.cpp      - HTTP视频流服务器
 * 
 * 功能概述：
 * 1. 远程拍照上传（1080p高清）
 * 2. MJPEG实时视频流
 * 3. LED闪光灯/指示灯控制
 * 4. DHT22温湿度监测
 * 5. 远程配置管理（WiFi/MQTT/上传URL）
 */

#include "config.h"

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
      config.fb_count = 1;
      config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
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
  ledcWrite(LED_PIN, 0);
  Serial.println("闪光灯PWM初始化完成");
#endif

  // 初始化红色指示灯
  pinMode(RED_LED_PIN, OUTPUT);
  digitalWrite(RED_LED_PIN, HIGH);  // HIGH = 关闭 (低电平有效)
  Serial.println("红色指示灯初始化完成");
  
  // 初始化DHT22传感器
  dht.begin();
  Serial.println("DHT22温湿度传感器初始化完成");

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

  // 定期上报设备状态（精简版，默认60秒）
  // 完整配置只在页面请求时发送（通过get_config指令）
  if (millis() - lastStatusReport >= statusReportInterval) {
    publishStatus();  // 只发送状态：uptime/freeHeap/rssi/ledStatus等
    lastStatusReport = millis();
  }
  
  // 按配置间隔读取DHT22温湿度(默认5秒)
  if (millis() - lastDhtReadTime >= dhtReadInterval) {
    readAndPublishDHT();
    lastDhtReadTime = millis();
  }

  delay(10);
}