package com.github.jojotech.spring.boot.starter.redis.related.aop;

/**
 * redisson 切点通知
 */
public class RedissonRateLimiterAdvisor extends AbstractRedissonAdvisor<RedissonRateLimiterProperties> {
    public RedissonRateLimiterAdvisor(RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut) {
        super(redissonRateLimiterCachedPointcut);
    }
}
