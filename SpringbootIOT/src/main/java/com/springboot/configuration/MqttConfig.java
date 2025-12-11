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
        options.setServerURIs(new String[]{url}); //mqtt地址
        // options.setUserName("user"); //账户密码
        // options.setPassword("pass".toCharArray());
        // options.setCleanSession(true); //是否不需要离线消息
        factory.setConnectionOptions(options);
        return factory;
    }

    /* 发送通道：服务器→ESP */
    @Bean
    @ServiceActivator(inputChannel = "cmdOutboundChannel")
    public MessageHandler cmdOutbound(MqttPahoClientFactory factory) {
        //指定服务器发布端的客户端ID(在Broker中显示的)和MQTT客户端工厂
        MqttPahoMessageHandler h = new MqttPahoMessageHandler("spring-cam-cmd", factory);
        h.setAsync(true); //异步发送
        return h;
    }

    /* 接收通道：ESP→服务器 */
    @Bean
    public MessageProducer inbound(MqttPahoClientFactory factory) {
        //指定服务器订阅端的客户端ID(在Broker中显示的)和MQTT客户端工厂和接收的主题 + 是通配符
        MqttPahoMessageDrivenChannelAdapter a =
                new MqttPahoMessageDrivenChannelAdapter("spring-cam-result", factory, 
                    "cam/+/upload", "cam/+/result", "cam/+/status", "cam/+/dht", "cam/+/config");
        a.setCompletionTimeout(5000); //发送订阅请求的超时时间
        a.setQos(1); //确认机制 至少一次
        a.setOutputChannel(mqttInputChannel()); //指定接收通道
        return a;
    }

    @Bean
    public MessageChannel cmdOutboundChannel() {return new DirectChannel();}
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
}