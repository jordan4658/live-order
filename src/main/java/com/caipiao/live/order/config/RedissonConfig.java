package com.caipiao.live.order.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${spring.redis.cluster.nodes}")
	private String nodes;
	@Value("${spring.redis.password}")
	private String password;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		ClusterServersConfig serverConfig = config.useClusterServers();
		serverConfig.setScanInterval(2000); // 集群状态扫描间隔时间，单位是毫秒
		if (StringUtils.isNotEmpty(nodes)) {
			String str[] = nodes.split(",");
			for (int i = 0; i < str.length; i++) {
				log.info("redis addr : [{}]", str[i]);
				if (StringUtils.isNotEmpty(str[i])) {
					serverConfig.addNodeAddress("redis://" + str[i].trim());//// use "rediss://" for SSL connection
				}
			}
		} else {
			log.error("REDIS集群地址为空,系统退出");
			System.exit(0);
		}

		// 设置密码
		if (StringUtils.isNotEmpty(password)) {
			serverConfig.setPassword(password);
		}
		return Redisson.create(config);
	}

}
