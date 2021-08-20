package com.github.jojotech.spring.boot.starter.redis.related.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁名称注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface RedissonLockName {
    String DEFAULT_PREFIX = "redisson:lock:";

    String prefix() default DEFAULT_PREFIX;

    String expression() default "";
}
