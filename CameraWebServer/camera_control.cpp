/**
 * @file camera_control.cpp
 * @brief 摄像头控制模块
 * 
 * 负责：
 * - 拍照功能
 * - HTTP上传图片到后端
 * - 摄像头参数调整
 */

#include "config.h"

/**
 * 拍照并上传
 */
void captureAndUpload(long cmdId) {
  Serial.println("开始拍照上传...");
  Serial.printf("收到指令ID: %ld\n", cmdId);
  
  // 直接拍照
  camera_fb_t *fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("摄像头拍照失败");
    publishResult(cmdId, false, "拍照失败");
    return;
  }

  Serial.printf("已拍照片: %d 字节\n", fb->len);
  uploadImage(fb, cmdId);
  esp_camera_fb_return(fb);
}

/**
 * HTTP上传图片到后端（分块传输，节省内存）
 */
void uploadImage(camera_fb_t *fb, long cmdId) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi未连接，上传失败");
    publishResult(cmdId, false, "WiFi未连接");
    return;
  }

  Serial.println("开始上传图片（分块传输）...");
  
  char fileName[64];
  snprintf(fileName, sizeof(fileName), "%s_%ld.jpg", mqtt_client_id.c_str(), cmdId);
  Serial.printf("文件名: %s\n", fileName);
  Serial.printf("图片大小: %d 字节\n", fb->len);

  // 解析upload_url获取host和path
  String url = upload_url;
  String host, path;
  int port = 80;
  
  if (url.startsWith("http://")) {
    url = url.substring(7);
  }
  int pathIndex = url.indexOf('/');
  if (pathIndex > 0) {
    host = url.substring(0, pathIndex);
    path = url.substring(pathIndex);
  } else {
    host = url;
    path = "/";
  }
  
  // 检查是否有端口号
  int portIndex = host.indexOf(':');
  if (portIndex > 0) {
    port = host.substring(portIndex + 1).toInt();
    host = host.substring(0, portIndex);
  }
  
  Serial.printf("连接到: %s:%d%s\n", host.c_str(), port, path.c_str());
  
  WiFiClient client;
  client.setTimeout(30);  // 30秒超时
  
  if (!client.connect(host.c_str(), port)) {
    Serial.println("✗ 连接服务器失败");
    publishResult(cmdId, false, "连接失败");
    return;
  }
  
  // 构建multipart边界
  String boundary = "----ESP32CamBoundary";
  
  // 第一部分：文件名字段
  String part1 = "--" + boundary + "\r\n";
  part1 += "Content-Disposition: form-data; name=\"fileName\"\r\n\r\n";
  part1 += String(fileName) + "\r\n";
  
  // 第二部分头：文件字段开始
  String part2Header = "--" + boundary + "\r\n";
  part2Header += "Content-Disposition: form-data; name=\"file\"; filename=\"" + String(fileName) + "\"\r\n";
  part2Header += "Content-Type: image/jpeg\r\n\r\n";
  
  // 结束边界
  String endBoundary = "\r\n--" + boundary + "--\r\n";
  
  // 计算总长度
  size_t contentLength = part1.length() + part2Header.length() + fb->len + endBoundary.length();
  
  // 发送HTTP请求头
  client.printf("POST %s HTTP/1.1\r\n", path.c_str());
  client.printf("Host: %s\r\n", host.c_str());
  client.printf("Content-Type: multipart/form-data; boundary=%s\r\n", boundary.c_str());
  client.printf("Content-Length: %d\r\n", contentLength);
  client.print("Connection: close\r\n\r\n");
  
  Serial.println("发送第1块: 表单头...");
  // 发送第1块：表单头部分
  client.print(part1);
  client.print(part2Header);
  delay(10);
  
  Serial.println("发送第2块: 图片数据...");
  // 发送第2块：图片数据（分小块发送）
  size_t chunkSize = 4096;  // 每块4KB
  size_t sent = 0;
  while (sent < fb->len) {
    size_t toSend = min(chunkSize, fb->len - sent);
    size_t written = client.write(fb->buf + sent, toSend);
    if (written == 0) {
      Serial.println("✗ 发送图片数据失败");
      publishResult(cmdId, false, "发送失败");
      client.stop();
      return;
    }
    sent += written;
    delay(1);  // 小延迟避免缓冲区溢出
  }
  Serial.printf("已发送图片: %d 字节\n", sent);
  
  // 发送结束边界
  client.print(endBoundary);
  Serial.println("发送完成，等待响应...");
  
  // 等待响应
  unsigned long timeout = millis() + 15000;
  while (client.connected() && !client.available()) {
    if (millis() > timeout) {
      Serial.println("✗ 等待响应超时");
      publishResult(cmdId, false, "响应超时");
      client.stop();
      return;
    }
    delay(10);
  }
  
  // 读取响应
  String statusLine = client.readStringUntil('\n');
  Serial.println("响应: " + statusLine);
  
  if (statusLine.indexOf("200") > 0) {
    Serial.println("✓ 上传成功！");
    publishResult(cmdId, true, "上传成功");
  } else {
    Serial.println("✗ 上传失败");
    publishResult(cmdId, false, "上传失败");
  }
  
  client.stop();
  Serial.println("上传完成");
}

/**
 * 设置摄像头参数
 */
void setCameraParam(const char* param, int value) {
  sensor_t *s = esp_camera_sensor_get();
  
  // 分辨率和质量
  if (strcmp(param, "framesize") == 0) {
    s->set_framesize(s, (framesize_t)value);
  } else if (strcmp(param, "quality") == 0) {
    s->set_quality(s, value);
  } 
  // 图像调整
  else if (strcmp(param, "brightness") == 0) {
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
  } 
  // 白平衡
  else if (strcmp(param, "awb") == 0) {
    s->set_whitebal(s, value);
  } else if (strcmp(param, "awb_gain") == 0) {
    s->set_awb_gain(s, value);
  } else if (strcmp(param, "wb_mode") == 0) {
    s->set_wb_mode(s, value);
  } 
  // 曝光控制
  else if (strcmp(param, "aec") == 0) {
    s->set_exposure_ctrl(s, value);
  } else if (strcmp(param, "aec2") == 0) {
    s->set_aec2(s, value);
  } else if (strcmp(param, "ae_level") == 0) {
    s->set_ae_level(s, value);
  } else if (strcmp(param, "aec_value") == 0) {
    s->set_aec_value(s, value);
  } 
  // 增益控制
  else if (strcmp(param, "agc") == 0) {
    s->set_gain_ctrl(s, value);
  } else if (strcmp(param, "agc_gain") == 0) {
    s->set_agc_gain(s, value);
  } else if (strcmp(param, "gainceiling") == 0) {
    s->set_gainceiling(s, (gainceiling_t)value);
  } 
  // 图像校正
  else if (strcmp(param, "bpc") == 0) {
    s->set_bpc(s, value);
  } else if (strcmp(param, "wpc") == 0) {
    s->set_wpc(s, value);
  } else if (strcmp(param, "raw_gma") == 0) {
    s->set_raw_gma(s, value);
  } else if (strcmp(param, "lenc") == 0) {
    s->set_lenc(s, value);
  } 
  // 图像翻转
  else if (strcmp(param, "hmirror") == 0) {
    s->set_hmirror(s, value);
  } else if (strcmp(param, "vflip") == 0) {
    s->set_vflip(s, value);
  } 
  // DCW (降采样)
  else if (strcmp(param, "dcw") == 0) {
    s->set_dcw(s, value);
  } else if (strcmp(param, "colorbar") == 0) {
    s->set_colorbar(s, value);
  }
  
  Serial.printf("摄像头参数 %s 设置为 %d\n", param, value);
}

