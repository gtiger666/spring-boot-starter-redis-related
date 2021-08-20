package com.github.jojotech.spring.boot.starter.redis.related.aop;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * redisson 切点通知
 */
public class RedissonAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private CachedPointcut cachedPointcut;

    public RedissonAdvisor(CachedPointcut cachedPointcut) {
        this.cachedPointcut = cachedPointcut;
    }

    @Override
    public Pointcut getPointcut() {
        return this.cachedPointcut;
    }
}
