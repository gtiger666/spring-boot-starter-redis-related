package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import lombok.Getter;

@Getter
public class RedissonLockProperties extends AbstractRedissonProperties {
    private final RedissonLock redissonLock;
    private final RedissonLockName redissonLockName;

    public RedissonLockProperties(RedissonLock redissonLock, RedissonLockName redissonLockName, int parameterIndex) {
        super(parameterIndex);
        this.redissonLock = redissonLock;
        this.redissonLockName = redissonLockName;
    }
}
