/**
 * @file asr_handler.cpp
 * @brief ASR PRO 语音模块串口通信处理
 * 
 * 负责：
 * - 串口2初始化 (仅接收，使用GPIO3)
 * - 接收ASR PRO发送的语音指令
 * - 执行对应的控制操作
 * - 通过MQTT发送执行结果给后端（记录日志+SSE推送）
 * 
 * 注意：GPIO16与ESP32-CAM的PSRAM冲突，不能使用！
 * 方案：使用GPIO3(U0RXD)作为接收引脚，与调试串口复用
 *       烧录完成后断开USB，使用GPIO3接收ASR PRO数据
 */

#include "config.h"

// ===========================
// 串口配置
// ===========================
// ASR PRO 连接 (单向: ASR PRO -> ESP32):
// - ASR PRO TX (PA_2) -> ESP32 GPIO15 (RX)
// 
// 注意: 只需单向通信，ESP32接收ASR PRO的指令即可
// ASR PRO的回复语音由自身TTS播放，无需ESP32发送
// GPIO16与PSRAM冲突不可用，GPIO4是LED引脚
#define ASR_RX_PIN 15  // ESP32接收 (连接ASR PRO的TX)
#define ASR_TX_PIN -1  // 不使用TX（单向通信）
#define ASR_BAUD_RATE 115200

// 接收缓冲区
#define ASR_BUFFER_SIZE 64
char asrBuffer[ASR_BUFFER_SIZE];
int asrBufferIndex = 0;

/**
 * 初始化ASR PRO串口通信
 */
void initASR() {
  // 使用Serial2，GPIO15作为RX（单向通信）
  Serial2.begin(ASR_BAUD_RATE, SERIAL_8N1, ASR_RX_PIN, ASR_TX_PIN);
  Serial.println("ASR PRO串口初始化完成 (GPIO15 RX, 115200bps)");
}

/**
 * 处理ASR PRO发来的语音指令
 * @param cmd 指令字符串 (如 "LED_ON", "FAN_ON" 等)
 */
void handleASRCommand(const char* cmd) {
  Serial.printf("收到ASR PRO语音指令: %s\n", cmd);
  
  // ===== LED控制 =====
  if (strcmp(cmd, "LED_ON") == 0) {
    controlLED(1);
    publishResult(0, true, "语音控制: LED开启");
  }
  else if (strcmp(cmd, "LED_OFF") == 0) {
    controlLED(0);
    publishResult(0, true, "语音控制: LED关闭");
  }
  else if (strcmp(cmd, "LED_MAX") == 0) {
    setLEDBrightness(255);
    controlLED(1);  // 确保LED开启
    publishResult(0, true, "语音控制: LED最亮");
  }
  else if (strcmp(cmd, "LED_MID") == 0) {
    setLEDBrightness(128);
    controlLED(1);  // 确保LED开启
    publishResult(0, true, "语音控制: LED中等亮度");
  }
  else if (strcmp(cmd, "RLED_ON") == 0) {
    controlRedLED(1);
    publishResult(0, true, "语音控制: 指示灯开启");
  }
  else if (strcmp(cmd, "RLED_OFF") == 0) {
    controlRedLED(0);
    publishResult(0, true, "语音控制: 指示灯关闭");
  }
  
  // ===== 窗户控制 =====
  else if (strcmp(cmd, "WIN_OPEN") == 0) {
    setServoAngle(180);
    publishResult(0, true, "语音控制: 窗户打开");
  }
  else if (strcmp(cmd, "WIN_CLOSE") == 0) {
    setServoAngle(0);
    publishResult(0, true, "语音控制: 窗户关闭");
  }
  
  // ===== 风扇控制 =====
  else if (strcmp(cmd, "FAN_ON") == 0) {
    controlRelay(true);
    publishResult(0, true, "语音控制: 风扇开启");
  }
  else if (strcmp(cmd, "FAN_OFF") == 0) {
    controlRelay(false);
    publishResult(0, true, "语音控制: 风扇关闭");
  }
  
  // ===== 摄像头控制 =====
  else if (strcmp(cmd, "CAPTURE") == 0) {
    publishResult(0, true, "语音控制: 正在拍照");
    captureAndUpload(0);  // cmdId为0表示语音控制
  }
  
  else {
    Serial.printf("未知的ASR指令: %s\n", cmd);
  }
}

/**
 * 处理ASR PRO串口数据 (在loop中调用)
 */
void processASRSerial() {
  while (Serial2.available()) {
    char c = Serial2.read();
    
    if (c == '\n' || c == '\r') {
      if (asrBufferIndex > 0) {
        asrBuffer[asrBufferIndex] = '\0';
        
        // 检查是否是CMD:开头的指令
        if (strncmp(asrBuffer, "CMD:", 4) == 0) {
          handleASRCommand(asrBuffer + 4);  // 跳过"CMD:"前缀
        }
        
        asrBufferIndex = 0;
      }
    } else if (asrBufferIndex < ASR_BUFFER_SIZE - 1) {
      asrBuffer[asrBufferIndex++] = c;
    }
  }
}
