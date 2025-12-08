package com.springboot.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@IntegrationComponentScan
public class MqttConfig {
    @Value("${mqtt.url}")
    private String url;


    /* MQTT客户端工厂 */
    @Bean
    public MqttPahoClientFactory factory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{url});
        // options.setUserName("user");
        // options.setPassword("pass".toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    /* 下行通道：服务器→ESP */
    @Bean
    @ServiceActivator(inputChannel = "cmdOutboundChannel")
    public MessageHandler cmdOutbound(MqttPahoClientFactory factory) {
        MqttPahoMessageHandler h = new MqttPahoMessageHandler("spring-cam-cmd", factory);
        h.setAsync(true);
        return h;
    }

    /* 上行通道：ESP→服务器 */
    @Bean
    public MessageProducer inbound(MqttPahoClientFactory factory) {
        MqttPahoMessageDrivenChannelAdapter a =
                new MqttPahoMessageDrivenChannelAdapter("spring-cam-result", factory, 
                    "cam/+/upload", "cam/+/result", "cam/+/status");  // 添加status订阅
        a.setCompletionTimeout(5000);
        a.setQos(1);
        a.setOutputChannel(mqttInputChannel());
        return a;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
}