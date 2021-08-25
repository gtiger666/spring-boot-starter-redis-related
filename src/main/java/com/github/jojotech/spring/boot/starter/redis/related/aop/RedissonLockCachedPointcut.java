package com.github.jojotech.spring.boot.starter.redis.related.aop;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class RedissonLockCachedPointcut extends AbstractRedissonCachePointcut<RedissonLockProperties> {
    protected RedissonLockProperties computeRedissonProperties(Method method, Class<?> clazz) {
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
                        return new RedissonLockProperties(l, op.get(), i);
                    }
                }
                if (StringUtils.isNotBlank(l.name())){
                    return new RedissonLockProperties(l, null, -1);
                }
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
}
