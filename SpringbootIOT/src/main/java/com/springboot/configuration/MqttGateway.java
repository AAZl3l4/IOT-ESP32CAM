package com.springboot.configuration;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

// MQTT消息发送网关
@MessagingGateway(defaultRequestChannel = "cmdOutboundChannel") // 统一指定本接口下的 默认的发送通道
public interface MqttGateway {
    // 发送消息 可使用@Gateway(requestChannel = "cmdOutboundChannel")覆盖默认的发送通道
    void send(@Header(MqttHeaders.TOPIC) String topic, String payload);
}