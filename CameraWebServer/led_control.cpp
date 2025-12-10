/**
 * @file led_control.cpp
 * @brief LED控制模块
 * 
 * 负责：
 * - 闪光灯开关控制（GPIO4）
 * - 闪光灯PWM亮度调节
 * - 红色指示灯开关控制（GPIO33）
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
bool ledStatus = false;
int ledBrightness = 128;
bool redLedStatus = false;

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
 * 红色指示灯开关控制 (GPIO 33, 低电平有效)
 */
void controlRedLED(int value) {
  redLedStatus = (value != 0);
  // 注意: GPIO 33 是低电平点亮，所以逻辑取反
  digitalWrite(RED_LED_PIN, redLedStatus ? LOW : HIGH);
  Serial.printf("红色指示灯 %s\n", redLedStatus ? "开启" : "关闭");
}
