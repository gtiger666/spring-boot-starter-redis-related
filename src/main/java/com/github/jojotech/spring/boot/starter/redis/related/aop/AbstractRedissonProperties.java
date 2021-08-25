package com.github.jojotech.spring.boot.starter.redis.related.aop;

public abstract class AbstractRedissonProperties {
    public static final Object NONE = new Object();

    private final int parameterIndex;

    protected AbstractRedissonProperties(int parameterIndex) {
        this.parameterIndex = parameterIndex;
    }

    /**
     * 参数坐标
     * @return
     */
    public int getParameterIndex() {
       return this.parameterIndex;
    }
}
