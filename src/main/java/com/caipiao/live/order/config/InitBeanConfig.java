package com.caipiao.live.order.config;


import com.caipiao.live.common.util.redis.BasicRedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitBeanConfig {

    @Bean
    public BasicRedisClient initBasicRedisClient() {
        return new BasicRedisClient();
    }
}