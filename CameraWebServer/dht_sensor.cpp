/**
 * @file dht_sensor.cpp
 * @brief DHT22温湿度传感器模块
 * 
 * 负责：
 * - DHT22传感器初始化
 * - 读取温湿度数据
 * - 通过MQTT发布温湿度数据
 */

#include "config.h"

// ===========================
// 全局变量定义
// ===========================
DHT dht(DHT_PIN, DHT_TYPE);
unsigned long lastDhtReadTime = 0;
unsigned long dhtReadInterval = 5000;  // DHT读取间隔(毫秒)，默认5秒

/**
 * 读取DHT22并发布温湿度和光照数据
 */
void readAndPublishDHT() {
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  
  // 检查读取是否成功
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("DHT22读取失败");
    return;
  }
  
  // 读取光敏传感器
  readLightSensor();
  
  // 构建JSON数据 (温湿度+光照明暗)
  StaticJsonDocument<128> doc;
  doc["clientId"] = mqtt_client_id;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["lightDark"] = lightDigitalValue;  // true=暗, false=亮
  
  char buffer[128];
  serializeJson(doc, buffer);
  
  // 发布到 cam/{clientId}/dht topic
  char topic[64];
  snprintf(topic, sizeof(topic), "cam/%s/dht", mqtt_client_id.c_str());
  
  mqttClient.publish(topic, buffer, 0);
  Serial.printf("温湿度: %.1f℃ %.1f%%, 光照:%s\n", temperature, humidity, lightDigitalValue ? "暗" : "亮");
}
