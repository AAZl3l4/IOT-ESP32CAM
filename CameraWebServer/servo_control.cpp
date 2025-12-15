/**
 * @file servo_control.cpp
 * @brief SG90舵机控制模块
 * 
 * 负责：
 * - 舵机PWM初始化 (GPIO14, 50Hz)
 * - 角度控制 (0-180度)
 * - 用于模拟窗户开关
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
int servoAngle = 0;  // 当前舵机角度 (0-180)

/**
 * 初始化舵机PWM
 * SG90舵机: 50Hz, 脉宽500-2500μs对应0-180度
 */
void initServo() {
  ledcAttach(SERVO_PIN, SERVO_PWM_FREQ, SERVO_PWM_RESOLUTION);
  setServoAngle(0);  // 初始位置0度(关闭)
  Serial.println("舵机PWM初始化完成 (GPIO14)");
}

/**
 * 设置舵机角度
 * @param angle 目标角度 (0-180)
 * 
 * 计算原理:
 * - SG90脉宽: 500μs(0°) ~ 2500μs(180°)
 * - PWM周期: 20ms (50Hz)
 * - 12位分辨率: 4096级
 * - duty = (500 + angle * 2000 / 180) * 4096 / 20000
 */
void setServoAngle(int angle) {
  // 限制角度范围
  if (angle < 0) angle = 0;
  if (angle > 180) angle = 180;
  
  servoAngle = angle;
  
  // 计算脉宽 (微秒): 500 + (angle / 180) * 2000
  // 范围: 500μs (0°) ~ 2500μs (180°)
  int pulseWidth = SERVO_MIN_PULSE + (angle * (SERVO_MAX_PULSE - SERVO_MIN_PULSE) / 180);
  
  // 转换为12位PWM占空比
  // duty_cycle = pulseWidth * 4096 / 20000
  int dutyCycle = pulseWidth * 4096 / 20000;
  
  ledcWrite(SERVO_PIN, dutyCycle);
  
  Serial.printf("舵机角度设为 %d° (脉宽: %dμs, 占空比: %d)\n", angle, pulseWidth, dutyCycle);
}

/**
 * 获取角度对应的窗户状态描述
 */
const char* getWindowStatus(int angle) {
  if (angle >= 170) return "全开";
  if (angle >= 80 && angle <= 100) return "半开";
  if (angle >= 35 && angle <= 55) return "小开";
  if (angle <= 10) return "关闭";
  return "自定义";
}
