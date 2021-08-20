package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import com.github.jojotech.spring.boot.starter.redis.related.domain.LockProperties;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import reactor.util.function.Tuples;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class CachedPointcut extends StaticMethodMatcherPointcut {

    /**
     * Key 为方法全限定名称 + 参数，value 为对应的 Redisson 锁注解以及锁名称
     */
    private final Map<String, LockProperties> cache = new ConcurrentHashMap<>();
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
            LockProperties lockProperties = computeLockProperties(method, targetClass);
            if (lockProperties != null) {
                return lockProperties;
            }
            List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(targetClass);
            Optional<LockProperties> optional = fromClasses(allSuperclasses, method);
            if (optional.isEmpty()) {
                allSuperclasses = ClassUtils.getAllInterfaces(targetClass);
                optional = fromClasses(allSuperclasses, method);
            }
            return optional.orElse(LockProperties.NO_LOCK);
        }) != LockProperties.NO_LOCK;
    }

    public LockProperties getLockProperties(Method method, Class<?> targetClass) {
        return cache.get(key(method, targetClass));
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
    private Optional<LockProperties> fromClasses(List<Class<?>> list, Method method) {
        return list.stream()
                .map(i -> computeLockProperties(method, i))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private LockProperties computeLockProperties(Method method, Class<?> clazz) {
        try {
            Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
            RedissonLock l = m.getAnnotation(RedissonLock.class);
            if (l != null) {
                Annotation[][] as = method.getParameterAnnotations();
                for (int i = 0; i < as.length; i++) {
                    Annotation[] ar = as[i];
                    if (ArrayUtils.isEmpty(ar)) {
                        continue;
                    }
                    //获取第一个 RedissonLockName 注解的参数
                    Optional<RedissonLockName> op = Arrays.stream(ar)
                            .filter(a -> a instanceof RedissonLockName)
                            .map(a -> (RedissonLockName) a)
                            .findFirst();
                    if (op.isPresent()) {
                        return new LockProperties(l, op.get(), i);
                    }
                }
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
}
