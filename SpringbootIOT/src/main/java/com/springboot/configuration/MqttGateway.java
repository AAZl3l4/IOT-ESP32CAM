package com.springboot.configuration;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

// MQTT消息发送接口
@MessagingGateway(defaultRequestChannel = "cmdOutboundChannel")
public interface MqttGateway {
    void send(@Header(MqttHeaders.TOPIC) String topic, String payload);
}