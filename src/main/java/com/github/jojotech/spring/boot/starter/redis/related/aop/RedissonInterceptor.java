package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import com.github.jojotech.spring.boot.starter.redis.related.domain.LockProperties;
import com.github.jojotech.spring.boot.starter.redis.related.exception.RedisRelatedException;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.LockOptions;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * redisson 锁核心实现类
 */
@Log4j2
public class RedissonInterceptor implements MethodInterceptor {
    private final RedissonClient redissonClient;
    private final CachedPointcut cachedPointcut;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParserContext context = new TemplateParserContext();

    public RedissonInterceptor(RedissonClient redissonClient, CachedPointcut cachedPointcut) {
        this.redissonClient = redissonClient;
        this.cachedPointcut = cachedPointcut;
    }

    /**
     * 新的 local name支持前缀及el表达式
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> clazz = invocation.getThis().getClass();
        LockProperties lockProperties = cachedPointcut.getLockProperties(method, clazz);
        if (lockProperties == null) {
            log.error("RedissonInterceptor-invoke error! Cannot find corresponding LockProperties, method {} run without lock", method.getName());
            return invocation.proceed();
        }
        String lockName = getLockName(lockProperties, invocation.getArguments());
        RedissonLock redissonLock = lockProperties.getRedissonLock();
        try {
            log.debug("RedissonInterceptor-invoke begin to try redisson lockName {}, method: {}, thread: {}", lockName, method.getName(), Thread.currentThread().getName());
            //创建锁
            RLock lock = null;
            RedissonLock.LockFeature lockFeature = redissonLock.lockFeature();
            if (lockFeature == RedissonLock.LockFeature.DEFAULT) {
                lock = redissonClient.getLock(lockName);
            } else if (lockFeature == RedissonLock.LockFeature.FAIR) {
                lock = redissonClient.getFairLock(lockName);
            } else if (lockFeature == RedissonLock.LockFeature.SPIN) {
                RedissonLock.BackOffType backOffType = redissonLock.backOffType();
                if (backOffType == RedissonLock.BackOffType.CONSTANT) {
                    lock = redissonClient.getSpinLock(lockName, new LockOptions.ConstantBackOff()
                            .delay(redissonLock.backOffDelay()));
                } else if (backOffType == RedissonLock.BackOffType.EXPONENTIAL) {
                    lock = redissonClient.getSpinLock(lockName, new LockOptions.ExponentialBackOff()
                            .initialDelay(redissonLock.backOffInitialDelay())
                            .maxDelay(redissonLock.backOffMaxDelay())
                            .multiplier(redissonLock.backOffMultiplier()));
                } else {
                    throw new RedisRelatedException("Not implemented BackOffType: " + backOffType);
                }
            } else if (lockFeature == RedissonLock.LockFeature.READ_WRITE) {
                if (redissonLock.readOrWrite() == RedissonLock.ReadOrWrite.READ) {
                    lock = redissonClient.getReadWriteLock(lockName).readLock();
                } else {
                    lock = redissonClient.getReadWriteLock(lockName).writeLock();
                }
            } else {
                throw new RedisRelatedException("Not implemented LockFeature: " + lockFeature);
            }
            if (lock == null) {
                log.error("RedissonInterceptor-invoke {} err! error during create redisson lock!", method.getName());
                return invocation.proceed();
            }
            try {
                boolean getLock = false;
                switch (redissonLock.lockType()) {
                    case RedissonLock.BLOCK_LOCK:
                        lock.lock(redissonLock.leaseTime(), redissonLock.timeUnit()); //默认为-1，永久持有直接主动释放
                        getLock = true;
                        break;
                    case RedissonLock.TRY_LOCK_NOWAIT:
                        getLock = lock.tryLock();
                        break;
                    case RedissonLock.TRY_LOCK:
                        getLock = lock.tryLock(redissonLock.waitTime(), redissonLock.leaseTime(), redissonLock.timeUnit());
                        break;
                }
                if (!getLock) {
                    throw new RedisRelatedException("can not get redisson lock,method:" + method.getName() + ", params: " + Arrays.toString(invocation.getArguments()));
                } else {
                    log.debug("RedissonInterceptor-invoke successfully locked lockName {}, method: {}, threadId: {}",
                            lockName, method.getName(), Thread.currentThread().getId());
                }
                return invocation.proceed();
            } finally {
                release(lock, method);
            }

        } catch (RedisRelatedException e) {
            log.info(e.getMessage());
            throw e;
        }
    }

    private void release(RLock lock, Method method) {
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("RedissonInterceptor-release redisson lock released, method:" + method.getName()
                        + ", threadId:" + Thread.currentThread().getId());
            } catch (Exception e) {
                log.error("error during release redisson lock", e);
            }
        } else {
            log.debug("RedissonInterceptor-release redisson lock not exist or held, method:"
                    + method.getName() + ", threadId:" + Thread.currentThread().getId());
        }
    }

    private String getLockName(LockProperties lockProperties, Object... params) {
        RedissonLockName redissonLockName = lockProperties.getRedissonLockName();
        int parameterIndex = lockProperties.getParameterIndex();
        StringBuilder lockName = new StringBuilder();
        String prefix = redissonLockName.prefix();
        String expression = redissonLockName.expression();
        if (StringUtils.isNotBlank(expression)) {
            lockName.append(prefix).append(parser.parseExpression(expression, context).getValue(params[parameterIndex]));
        } else {
            lockName.append(prefix).append(params[parameterIndex]);
        }
        return lockName.toString();
    }
}
