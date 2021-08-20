package com.github.jojotech.spring.boot.starter.redis.related.domain;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockProperties {
    public static final LockProperties NO_LOCK = new LockProperties();

    private RedissonLock redissonLock;
    private RedissonLockName redissonLockName;
    private int parameterIndex;
}
