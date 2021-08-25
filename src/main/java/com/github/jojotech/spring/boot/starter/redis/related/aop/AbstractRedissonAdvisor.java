package com.github.jojotech.spring.boot.starter.redis.related.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

public abstract class AbstractRedissonAdvisor<RedissonProp extends AbstractRedissonProperties> extends AbstractBeanFactoryPointcutAdvisor {
    private final AbstractRedissonCachePointcut<RedissonProp> abstractRedissonCachePointcut;

    protected AbstractRedissonAdvisor(AbstractRedissonCachePointcut<RedissonProp> abstractRedissonCachePointcut) {
        this.abstractRedissonCachePointcut = abstractRedissonCachePointcut;
    }

    @Override
    public Pointcut getPointcut() {
        return abstractRedissonCachePointcut;
    }
}
