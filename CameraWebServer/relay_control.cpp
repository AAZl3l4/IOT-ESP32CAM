/**
 * @file relay_control.cpp
 * @brief 继电器控制模块 (风扇)
 * 
 * GPIO4 控制继电器开关
 * 高电平=继电器吸合=风扇转动
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
bool relayStatus = false;  // 继电器状态 (true=开, false=关)

/**
 * 初始化继电器
 */
void initRelay() {
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);  // 默认关闭
  relayStatus = false;
  Serial.printf("继电器初始化完成 (GPIO%d)\n", RELAY_PIN);
}

/**
 * 控制继电器开关
 * @param on true=开启, false=关闭
 */
void controlRelay(bool on) {
  relayStatus = on;
  digitalWrite(RELAY_PIN, on ? HIGH : LOW);
  Serial.printf("继电器: %s\n", on ? "开启" : "关闭");
}
