/**
 * @file camera_control.cpp
 * @brief 摄像头控制模块
 * 
 * 负责：
 * - 拍照功能（1080p高清）
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
  
  sensor_t *s = esp_camera_sensor_get();
  framesize_t old_framesize = s->status.framesize;
  
  // 切换到1080p拍照
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
  
  // 恢复原分辨率
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
  
  // 构建multipart/form-data请求
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
  
  // 分配内存
  uint8_t* postData = (uint8_t*)malloc(totalLen);
  if (postData == NULL) {
    Serial.println("✗ 内存分配失败");
    publishResult(cmdId, false, "内存不足");
    http.end();
    return;
  }
  
  // 组装数据
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
