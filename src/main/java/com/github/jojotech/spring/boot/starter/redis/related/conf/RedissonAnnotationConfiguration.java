package com.github.jojotech.spring.boot.starter.redis.related.conf;

import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonLockAdvisor;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonLockCachedPointcut;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonLockInterceptor;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonRateLimiterAdvisor;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonRateLimiterCachedPointcut;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonRateLimiterInterceptor;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedissonAnnotationConfiguration {

    @Bean
    public RedissonLockCachedPointcut redissonLockCachedPointcut() {
        return new RedissonLockCachedPointcut();
    }

    @Bean
    public RedissonLockInterceptor redissonLockInterceptor(RedissonClient redissonClient, RedissonLockCachedPointcut redissonLockCachedPointcut) {
        return new RedissonLockInterceptor(redissonClient, redissonLockCachedPointcut);
    }

    @Bean
    public RedissonLockAdvisor redissonLockAdvisor(RedissonLockCachedPointcut redissonLockCachedPointcut, RedissonLockInterceptor redissonLockInterceptor, RedissonAopConfiguration redissonAopConfiguration) {
        var advisor = new RedissonLockAdvisor(redissonLockCachedPointcut);
        advisor.setAdvice(redissonLockInterceptor);
        advisor.setOrder(redissonAopConfiguration.getOrder());
        return advisor;
    }

    @Bean
    public RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut() {
        return new RedissonRateLimiterCachedPointcut();
    }

    @Bean
    public RedissonRateLimiterInterceptor redissonRateLimiterInterceptor(RedissonClient redissonClient, RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut) {
        return new RedissonRateLimiterInterceptor(redissonClient, redissonRateLimiterCachedPointcut);
    }

    @Bean
    public RedissonRateLimiterAdvisor redissonRateLimiterAdvisor(RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut, RedissonRateLimiterInterceptor redissonRateLimiterInterceptor, RedissonAopConfiguration redissonAopConfiguration) {
        var advisor = new RedissonRateLimiterAdvisor(redissonRateLimiterCachedPointcut);
        advisor.setAdvice(redissonRateLimiterInterceptor);
        advisor.setOrder(redissonAopConfiguration.getOrder());
        return advisor;
    }
}
