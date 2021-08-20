package com.github.jojotech.spring.boot.starter.redis.related.autoconf;

import com.github.jojotech.spring.boot.starter.redis.related.conf.LettuceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Lettuce
 */
@Configuration(proxyBeanMethods = false)
@Import({LettuceConfiguration.class})
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class LettuceAutoConfiguration {
}
