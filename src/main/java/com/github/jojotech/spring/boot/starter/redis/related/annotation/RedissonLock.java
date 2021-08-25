package com.github.jojotech.spring.boot.starter.redis.related.annotation;

import org.redisson.api.LockOptions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 在方法或者类上添加该注释后，自动添加基于 Redisson 的分布式锁
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
        {ElementType.METHOD, ElementType.TYPE}
)
public @interface RedissonLock {
    /**
     * 一般通过 RedissonLockName 指定锁名称
     * 但如果锁和方法参数无关，则通过这个 name 指定
     * 如果 RedissonLockName 为空，这个 name 也是默认的 空字符串，则锁不生效
     */
    String name() default "";

    /**
     * 锁特性
     */
    LockFeature lockFeature() default LockFeature.DEFAULT;

    /**
     * 阻塞锁
     */
    int BLOCK_LOCK = 1;
    /**
     * try lock，未获取则不等待，直接抛出 RedissionClientException
     */
    int TRY_LOCK_NOWAIT = 2;
    /**
     * try lock，包含等待
     */
    int TRY_LOCK = 3;

    /**
     * 锁类型
     */
    int lockType() default BLOCK_LOCK;

    /**
     * 锁等待时间
     */
    long waitTime() default 1000l;

    /**
     * 锁最长持有时间
     */
    long leaseTime() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    BackOffType backOffType() default BackOffType.EXPONENTIAL;

    /**
     * 这个参数在 LockFeature = SPIN， BackOffType = CONSTANT 使用
     */
    long backOffDelay() default 64L;

    /**
     * 以下三个参数在 LockFeature = SPIN， BackOffType = EXPONENTIAL 使用
     */
    long backOffMaxDelay()  default 128;
    long backOffInitialDelay() default 1;
    int backOffMultiplier() default 2;

    /**
     * 这个参数在 LockFeature = READ_WRITE 使用
     */
    ReadOrWrite readOrWrite() default ReadOrWrite.READ;

    enum LockFeature {
        /**
         * @see org.redisson.api.RedissonClient#getLock(String)
         */
        DEFAULT,
        /**
         * @see org.redisson.api.RedissonClient#getFairLock(String)
         */
        FAIR,
        /**
         * @see org.redisson.api.RedissonClient#getSpinLock(String)
         * @see org.redisson.api.RedissonClient#getSpinLock(String, LockOptions.BackOff)
         */
        SPIN,
        /**
         * @see org.redisson.api.RedissonClient#getReadWriteLock(String)
         */
        READ_WRITE,
        ;
    }

    enum BackOffType {
        /**
         * @see org.redisson.api.LockOptions.ConstantBackOff
         */
        CONSTANT,
        /**
         * @see org.redisson.api.LockOptions.ExponentialBackOff
         */
        EXPONENTIAL,
        ;
    }

    enum ReadOrWrite {
        READ,
        WRITE,
        ;
    }
}
