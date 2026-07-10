package com.example.Notification_Service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "event.exchange";
    public static final String ROUTING_KEY = "event.created";
}
