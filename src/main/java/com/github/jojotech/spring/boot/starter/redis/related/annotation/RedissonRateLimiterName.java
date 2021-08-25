package com.github.jojotech.spring.boot.starter.redis.related.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface RedissonRateLimiterName {
    String DEFAULT_PREFIX = "redisson:rateLimiter:";

    String prefix() default DEFAULT_PREFIX;

    String expression() default "";
}
