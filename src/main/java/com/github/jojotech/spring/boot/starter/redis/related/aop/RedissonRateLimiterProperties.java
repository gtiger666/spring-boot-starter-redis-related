package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiter;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiterName;
import lombok.Getter;

@Getter
public class RedissonRateLimiterProperties extends AbstractRedissonProperties {
    private final RedissonRateLimiter redissonRateLimiter;
    private final RedissonRateLimiterName redissonRateLimiterName;

    public RedissonRateLimiterProperties(RedissonRateLimiter redissonRateLimiter, RedissonRateLimiterName redissonRateLimiterName, int parameterIndex) {
        super(parameterIndex);
        this.redissonRateLimiter = redissonRateLimiter;
        this.redissonRateLimiterName = redissonRateLimiterName;
    }
}
