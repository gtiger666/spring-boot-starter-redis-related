package com.github.jojotech.spring.boot.starter.redis.related.aop;

/**
 * redisson 切点通知
 */
public class RedissonLockAdvisor extends AbstractRedissonAdvisor<RedissonLockProperties> {
    public RedissonLockAdvisor(RedissonLockCachedPointcut redissonLockCachedPointcut) {
        super(redissonLockCachedPointcut);
    }
}
