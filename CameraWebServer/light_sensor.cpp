/**
 * @file light_sensor.cpp
 * @brief 光敏电阻传感器模块 (仅DO数字输出)
 * 
 * 只使用DO输出判断环境明暗状态
 * DO引脚: GPIO2
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
bool lightDigitalValue = false;  // DO数字值 (true=暗, false=亮)

/**
 * 初始化光敏传感器
 */
void initLightSensor() {
  pinMode(LIGHT_DO_PIN, INPUT);
  Serial.printf("光敏传感器初始化完成 (DO:GPIO%d)\n", LIGHT_DO_PIN);
}

/**
 * 读取光敏传感器数据 (仅DO)
 */
void readLightSensor() {
  lightDigitalValue = digitalRead(LIGHT_DO_PIN);
  Serial.printf("光敏传感器: %s\n", lightDigitalValue ? "暗" : "亮");
}
