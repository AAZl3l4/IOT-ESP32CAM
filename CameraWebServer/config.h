/**
 * @file config.h
 * @brief ESP32-CAM 全局配置文件
 * 
 * 包含：
 * - 默认配置常量（WiFi、MQTT、上传URL）
 * - 硬件引脚定义（LED、DHT22）
 * - 全局变量声明
 * - 函数声明
 */

#ifndef CONFIG_H
#define CONFIG_H

#include <Arduino.h>
#include "esp_camera.h"
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <Preferences.h>
#include <DHT.h>

// 选择摄像头型号
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

// ===========================
// 硬件引脚定义
// ===========================
// 闪光灯 (GPIO 4 - 白色超亮)
#define LED_PIN 4
#define LED_PWM_CHANNEL 15
#define LED_PWM_FREQ 5000
#define LED_PWM_RESOLUTION 8  // 8位分辨率(0-255)

// 红色指示灯 (GPIO 33)
#define RED_LED_PIN 33

// DHT22温湿度传感器
#define DHT_PIN 13
#define DHT_TYPE DHT22

// SG90舵机 (GPIO14) - 用于窗户控制
#define SERVO_PIN 14
#define SERVO_PWM_FREQ 50        // 50Hz (20ms周期)
#define SERVO_PWM_RESOLUTION 12  // 12位分辨率 (0-4095)
#define SERVO_MIN_PULSE 500      // 0度对应500微秒
#define SERVO_MAX_PULSE 2500     // 180度对应2500微秒

// ===========================
// 全局变量声明 (extern)
// ===========================
// 配置变量
extern String wifi_ssid;
extern String wifi_password;
extern String mqtt_server;
extern int mqtt_port;
extern String mqtt_client_id;
extern String upload_url;

// MQTT客户端
extern WiFiClient espClient;
extern PubSubClient mqttClient;

// LED状态
extern bool ledStatus;
extern int ledBrightness;
extern bool redLedStatus;

// DHT传感器
extern DHT dht;
extern unsigned long lastDhtReadTime;
extern unsigned long dhtReadInterval;

// 状态上报
extern unsigned long lastStatusReport;
extern unsigned long statusReportInterval;

// 舵机状态
extern int servoAngle;

// Preferences对象
extern Preferences preferences;

// ===========================
// 函数声明
// ===========================
// app_httpd.cpp
void startCameraServer();

// config_manager.cpp - 配置管理
void loadConfig();
void saveWiFiConfig(const String& ssid, const String& pass);
void saveMQTTConfig(const String& server, int port, const String& clientId);
void saveUploadUrl(const String& url);
void resetConfig();

// mqtt_handler.cpp - MQTT处理
void setupMQTT();
void reconnectMQTT();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void handleCommand(StaticJsonDocument<512>& doc);

// camera_control.cpp - 摄像头控制
void captureAndUpload(long cmdId);
void uploadImage(camera_fb_t *fb, long cmdId);
void setCameraParam(const char* param, int value);

// led_control.cpp - LED控制
void controlLED(int value);
void setLEDBrightness(int value);
void controlRedLED(int value);

// dht_sensor.cpp - DHT传感器
void readAndPublishDHT();

// servo_control.cpp - 舵机控制
void initServo();
void setServoAngle(int angle);
const char* getWindowStatus(int angle);

// status_publisher.cpp - 状态发布
void publishResult(long cmdId, bool ok, const char* info);
void publishStatus();
void publishConfig(long cmdId = 0);

#endif // CONFIG_H
