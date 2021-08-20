package com.github.jojotech.spring.boot.starter.redis.related.autoconf;

import com.github.jojotech.spring.boot.starter.redis.related.conf.RedissonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Redisson 自动配置
 */
@Configuration(proxyBeanMethods = false)
@Import({RedissonConfiguration.class})
public class RedissonAutoConfiguration {
}
