package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public abstract class AbstractRedissonCachePointcut<RedissonProp extends AbstractRedissonProperties> extends StaticMethodMatcherPointcut {
    /**
     * Key 为方法全限定名称 + 参数，value 为对应的 Redisson 锁注解以及锁名称
     */
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * 判断该方法，或者父类（包含接口）中是否加锁，并缓存起来
     *
     * @param method
     * @param targetClass
     * @return
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        String key = key(method, targetClass);
        return cache.computeIfAbsent(key, k -> {
            RedissonProp redissonProp = computeRedissonProperties(method, targetClass);
            if (redissonProp != null) {
                return redissonProp;
            }
            List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(targetClass);
            Optional<RedissonProp> optional = fromClasses(allSuperclasses, method);
            if (optional.isEmpty()) {
                allSuperclasses = ClassUtils.getAllInterfaces(targetClass);
                optional = fromClasses(allSuperclasses, method);
            }
            if (optional.isPresent()) {
                return optional.get();
            }
            return AbstractRedissonProperties.NONE;
        }) != AbstractRedissonProperties.NONE;
    }

    public RedissonProp getRedissonProperties(Method method, Class<?> targetClass) {
        Object o = cache.get(key(method, targetClass));
        return (RedissonProp) o;
    }

    private String key(Method method, Class<?> targetClass) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(targetClass.getName()).append("#").append(method.getName()).append("(");
        Parameter[] parameters = method.getParameters();
        if (parameters != null && parameters.length > 0) {
            Arrays.stream(parameters).map(parameter -> parameter.getType().getName()).forEach(stringBuilder::append);
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * 从所有的父类方法中找到 {@link RedissonLock}
     *
     * @param list   所有的父类或接口
     * @param method 加锁的方法
     * @return {@link RedissonLock} {@link RedissonLockName} 以及{@link RedissonLockName} 所对应的参数下标
     */
    private Optional<RedissonProp> fromClasses(List<Class<?>> list, Method method) {
        return list.stream()
                .map(i -> computeRedissonProperties(method, i))
                .filter(Objects::nonNull)
                .findFirst();
    }

    protected abstract RedissonProp computeRedissonProperties(Method method, Class<?> clazz);
}
