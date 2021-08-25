package com.github.jojotech.spring.boot.starter.redis.related.test;

import com.github.jojotech.spring.boot.starter.redis.related.lettuce.MultiRedisLettuceConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import redis.embedded.RedisServer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.redis.enable-multi=true",
        "spring.redis.multi.default.host=127.0.0.1",
        "spring.redis.multi.default.port=6379",
        "spring.redis.multi.test.host=127.0.0.1",
        "spring.redis.multi.test.port=6380",
})
public class MultiRedisTest {
    private static RedisServer redisServer;
    private static RedisServer redisServer2;

    @BeforeAll
    public static void setUp() throws Exception {
        System.out.println("start redis");
        redisServer = RedisServer.builder().port(6379).setting("maxheap 200m").build();
        redisServer2 = RedisServer.builder().port(6380).setting("maxheap 200m").build();
        redisServer.start();
        redisServer2.start();
        System.out.println("redis started");
    }

    @AfterAll
    public static void tearDown() throws Exception {
        System.out.println("stop redis");
        redisServer.stop();
        redisServer2.stop();
        System.out.println("redis stopped");
    }

    @EnableAutoConfiguration
    @Configuration
    public static class App {
    }

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ReactiveStringRedisTemplate reactiveRedisTemplate;
    @Autowired
    private MultiRedisLettuceConnectionFactory multiRedisLettuceConnectionFactory;

    private void testMulti(String suffix) {
        redisTemplate.opsForValue().set("testDefault" + suffix, "testDefault");
        multiRedisLettuceConnectionFactory.setCurrentRedis("test");
        redisTemplate.opsForValue().set("testSecond" + suffix, "testSecond");
        Assertions.assertTrue(redisTemplate.hasKey("testDefault" + suffix));
        Assertions.assertFalse(redisTemplate.hasKey("testSecond" + suffix));
        multiRedisLettuceConnectionFactory.setCurrentRedis("test");
        Assertions.assertFalse(redisTemplate.hasKey("testDefault" + suffix));
        multiRedisLettuceConnectionFactory.setCurrentRedis("test");
        Assertions.assertTrue(redisTemplate.hasKey("testSecond" + suffix));
    }

    @Test
    public void testMultiBlock() {
        testMulti("");
    }

    @Test
    public void testMultiBlockMultiThread() throws InterruptedException {
        Thread thread[] = new Thread[50];
        AtomicBoolean result = new AtomicBoolean(true);
        for (int i = 0; i < thread.length; i++) {
            int finalI = i;
            thread[i] = new Thread(() -> {
                try {
                    testMulti("" + finalI);
                } catch (Exception e) {
                    e.printStackTrace();
                    result.set(false);
                }
            });
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].start();
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].join();
        }
        Assertions.assertTrue(result.get());
    }

    private Mono<Boolean> reactiveMulti(String suffix) {
        return reactiveRedisTemplate.opsForValue().set("testReactiveDefault" + suffix, "testReactiveDefault")
                .flatMap(b -> {
                    multiRedisLettuceConnectionFactory.setCurrentRedis("test");
                    return reactiveRedisTemplate.opsForValue().set("testReactiveSecond" + suffix, "testReactiveSecond");
                }).flatMap(b -> {
                    return reactiveRedisTemplate.hasKey("testReactiveDefault" + suffix);
                }).map(b -> {
                    Assertions.assertTrue(b);
                    System.out.println(Thread.currentThread().getName());
                    return b;
                }).flatMap(b -> {
                    return reactiveRedisTemplate.hasKey("testReactiveSecond" + suffix);
                }).map(b -> {
                    Assertions.assertFalse(b);
                    System.out.println(Thread.currentThread().getName());
                    return b;
                }).flatMap(b -> {
                    multiRedisLettuceConnectionFactory.setCurrentRedis("test");
                    return reactiveRedisTemplate.hasKey("testReactiveDefault" + suffix);
                }).map(b -> {
                    Assertions.assertFalse(b);
                    System.out.println(Thread.currentThread().getName());
                    return b;
                }).flatMap(b -> {
                    multiRedisLettuceConnectionFactory.setCurrentRedis("test");
                    return reactiveRedisTemplate.hasKey("testReactiveSecond" + suffix);
                }).map(b -> {
                    Assertions.assertTrue(b);
                    return b;
                });
    }

    @Test
    public void testMultiReactive() throws InterruptedException {
        for (int i = 0; i < 10000; i++) {
            reactiveMulti("" + i).subscribe(System.out::println);
        }
        TimeUnit.SECONDS.sleep(10);
    }
}
