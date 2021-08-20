package com.github.jojotech.spring.boot.starter.redis.related.conf;

import com.github.jojotech.spring.boot.starter.redis.related.aop.CachedPointcut;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonAdvisor;
import com.github.jojotech.spring.boot.starter.redis.related.aop.RedissonInterceptor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Qualifier("CustomizedRedissonConfiguration")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(value = {RedissonAopConfiguration.class})
public class RedissonConfiguration {
    @Autowired
    private RedissonAopConfiguration redissonAopConfiguration;

    @Bean
    public CachedPointcut redissonCachedPointCut() {
        return new CachedPointcut();
    }

    @Bean
    public RedissonInterceptor redissonInterceptor(RedissonClient redissonClient, CachedPointcut cachedPointcut) {
        return new RedissonInterceptor(redissonClient, cachedPointcut);
    }

    @Bean
    public RedissonAdvisor redissonAdvisor(CachedPointcut cachedPointcut, RedissonInterceptor redissonInterceptor) {
        var advisor = new RedissonAdvisor(cachedPointcut);
        advisor.setAdvice(redissonInterceptor);
        advisor.setOrder(redissonAopConfiguration.getOrder());
        return advisor;
    }
}
