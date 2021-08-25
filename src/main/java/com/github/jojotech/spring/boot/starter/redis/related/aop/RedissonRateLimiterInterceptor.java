package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.alibaba.fastjson.JSON;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiter;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiterName;
import com.github.jojotech.spring.boot.starter.redis.related.exception.RedisRelatedException;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * redisson 限流器核心实现类
 */
@Log4j2
public class RedissonRateLimiterInterceptor implements MethodInterceptor {

    private final RedissonClient redissonClient;
    private final RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParserContext context = new TemplateParserContext();

    public RedissonRateLimiterInterceptor(RedissonClient redissonClient, RedissonRateLimiterCachedPointcut redissonRateLimiterCachedPointcut) {
        this.redissonClient = redissonClient;
        this.redissonRateLimiterCachedPointcut = redissonRateLimiterCachedPointcut;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> clazz = invocation.getThis().getClass();
        RedissonRateLimiterProperties rateLimiterProperties = redissonRateLimiterCachedPointcut.getRedissonProperties(method, clazz);
        if (rateLimiterProperties == null || rateLimiterProperties == AbstractRedissonProperties.NONE) {
            log.error("RedissonRateLimiterInterceptor-invoke error! Cannot find corresponding RedissonRateLimiterProperties, method {} run without rateLimit", method.getName());
            return invocation.proceed();
        }
        RedissonRateLimiter redissonRateLimiter = rateLimiterProperties.getRedissonRateLimiter();
        String rateLimiterName = getRateLimiterName(rateLimiterProperties, invocation.getArguments());
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterName);
        rateLimiter.trySetRate(
                redissonRateLimiter.rateType(),
                redissonRateLimiter.rate(),
                redissonRateLimiter.rateInterval(),
                redissonRateLimiter.rateIntervalUnit()
        );
        RateLimiterConfig config = rateLimiter.getConfig();
        if (
                !config.getRate().equals(redissonRateLimiter.rate())
                        || !config.getRateInterval().equals(redissonRateLimiter.rateIntervalUnit().toMillis(redissonRateLimiter.rateInterval()))
                        || !config.getRateType().equals(redissonRateLimiter.rateType())
        ) {
            log.warn(
                    "RedissonRateLimiterInterceptor-invoke RRateLimiter config {} does not equal to current config {}, reset it. If this happens all the time, please check if you set various configuration for RRateLimiter of same name",
                    JSON.toJSONString(redissonRateLimiter), JSON.toJSONString(config)
            );
            rateLimiter.setRate(
                    redissonRateLimiter.rateType(),
                    redissonRateLimiter.rate(),
                    redissonRateLimiter.rateInterval(),
                    redissonRateLimiter.rateIntervalUnit()
            );
            config = rateLimiter.getConfig();
        }

        if (redissonRateLimiter.type() == RedissonRateLimiter.Type.BLOCK) {
            rateLimiter.acquire(redissonRateLimiter.permits());
        } else if (redissonRateLimiter.type() == RedissonRateLimiter.Type.TRY) {
            if (redissonRateLimiter.waitTime() < 0) {
                if (!rateLimiter.tryAcquire(redissonRateLimiter.permits())) {
                    throw new RedisRelatedException("Cannot acquire permits of RRateLimiter with name: " + rateLimiterName + ", rate: " + JSON.toJSONString(config));
                }
            } else {
                if (!rateLimiter.tryAcquire(redissonRateLimiter.permits(), redissonRateLimiter.waitTime(), redissonRateLimiter.timeUnit())) {
                    throw new RedisRelatedException("Cannot acquire permits of RRateLimiter with name: " + rateLimiterName + ", rate: " + JSON.toJSONString(config));
                }
            }
        }
        return invocation.proceed();
    }

    private String getRateLimiterName(RedissonRateLimiterProperties rateLimiterProperties, Object... params) {
        StringBuilder lockName = new StringBuilder();
        RedissonRateLimiterName redissonRateLimiterName = rateLimiterProperties.getRedissonRateLimiterName();
        if (redissonRateLimiterName != null) {
            int parameterIndex = rateLimiterProperties.getParameterIndex();
            String prefix = redissonRateLimiterName.prefix();
            String expression = redissonRateLimiterName.expression();
            if (StringUtils.isNotBlank(expression)) {
                lockName.append(prefix).append(parser.parseExpression(expression, context).getValue(params[parameterIndex]));
            } else {
                lockName.append(prefix).append(params[parameterIndex]);
            }
        } else {
            lockName.append(rateLimiterProperties.getRedissonRateLimiter().name());
        }
        return lockName.toString();
    }
}
