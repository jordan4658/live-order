package com.caipiao.live.order;


import com.caipiao.live.common.util.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableTransactionManagement
@EnableAsync
@ComponentScan(value = "com.caipiao.live")
@MapperScan({"com.caipiao.live.common.mybatis"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.caipiao.live")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class LiveOrder {

    private static final Logger logger = LoggerFactory.getLogger(LiveOrder.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LiveOrder.class);
//		application.setDefaultProperties(ApplicationConfig.processProperties());
        SpringUtil.setApplicationContext(application.run(args));
        logger.debug("OrderServerApplication startup ...");
    }

}
