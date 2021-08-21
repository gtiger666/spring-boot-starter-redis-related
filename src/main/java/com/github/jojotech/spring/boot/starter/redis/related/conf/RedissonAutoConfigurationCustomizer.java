package com.github.jojotech.spring.boot.starter.redis.related.conf;

import org.redisson.config.Config;

@FunctionalInterface
public interface RedissonAutoConfigurationCustomizer {

    void customize(final Config configuration);
}