package com.github.jojotech.spring.boot.starter.redis.related.test;

import com.alibaba.fastjson.JSON;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiter;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonRateLimiterName;
import lombok.Getter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.embedded.RedisServer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.redis.redisson.aop.order=" + RedissonRateLimiterTest.ORDER,
        "spring.redis.host=127.0.0.1",
        "spring.redis.port=6379",
})
public class RedissonRateLimiterTest {
    public static final int ORDER = -100000;
    private static final int THREAD_COUNT = 10;

    private static RedisServer redisServer;

    @BeforeAll
    public static void setUp() throws Exception {
        System.out.println("start redis");
        redisServer = RedisServer.builder().port(6379).setting("maxheap 200m").build();
        redisServer.start();
        System.out.println("redis started");
    }

    @AfterAll
    public static void tearDown() throws Exception {
        System.out.println("stop redis");
        redisServer.stop();
        System.out.println("redis stopped");
    }

    @EnableAutoConfiguration
    @Configuration
    public static class App {
        @Autowired
        private RedissonClient redissonClient;
        @Autowired
        private StringRedisTemplate redisTemplate;

        @Bean
        public TestRedissonRateLimiterClass testRedissonRateLimiterClass() {
            return new TestRedissonRateLimiterClass(redissonClient, redisTemplate);
        }
    }

    public static class TestRedissonRateLimiterClass {
        @Getter
        private final List<Long> list = new CopyOnWriteArrayList<>();
        private final RedissonClient redissonClient;
        private final StringRedisTemplate redisTemplate;
        @Getter
        private final AtomicBoolean result = new AtomicBoolean(true);

        public TestRedissonRateLimiterClass(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {
            this.redissonClient = redissonClient;
            this.redisTemplate = redisTemplate;
        }

        @RedissonRateLimiter(
                name = "testBlockAcquire",
                type = RedissonRateLimiter.Type.BLOCK,
                rate = 1,
                rateInterval = 1,
                rateType = RateType.OVERALL,
                rateIntervalUnit = RateIntervalUnit.SECONDS
        )
        public void testBlockAcquire() {
            try {
                list.add(System.currentTimeMillis());
                RRateLimiter testBlockAcquire = redissonClient.getRateLimiter("testBlockAcquire");
                RateLimiterConfig config = testBlockAcquire.getConfig();
                Assertions.assertEquals(1, config.getRate());
                Assertions.assertEquals(1000, config.getRateInterval());
                Assertions.assertEquals(RateType.OVERALL, config.getRateType());
                System.out.println(JSON.toJSONString(config));
            } catch (Exception e) {
                e.printStackTrace();
                result.set(false);
            }
        }

        @RedissonRateLimiter(
                name = "testBlockAcquireWithParams",
                type = RedissonRateLimiter.Type.BLOCK,
                rate = 1,
                rateInterval = 1,
                rateType = RateType.OVERALL,
                rateIntervalUnit = RateIntervalUnit.SECONDS
        )
        public void testBlockAcquireWithParams(@RedissonRateLimiterName String permitsName) {
            try {
                list.add(System.currentTimeMillis());
                RRateLimiter testBlockAcquire = redissonClient.getRateLimiter(RedissonRateLimiterName.DEFAULT_PREFIX + permitsName);
                RateLimiterConfig config = testBlockAcquire.getConfig();
                Assertions.assertEquals(1, config.getRate());
                Assertions.assertEquals(1000, config.getRateInterval());
                Assertions.assertEquals(RateType.OVERALL, config.getRateType());
                System.out.println(JSON.toJSONString(config));
            } catch (Exception e) {
                e.printStackTrace();
                result.set(false);
            }
        }

        @RedissonRateLimiter(
                name = "testTryAcquireNoWait",
                type = RedissonRateLimiter.Type.TRY,
                rate = 1,
                rateInterval = 1,
                rateType = RateType.OVERALL,
                rateIntervalUnit = RateIntervalUnit.SECONDS
        )
        public void testTryAcquireNoWait() {
            try {
                list.add(System.currentTimeMillis());
                RRateLimiter testBlockAcquire = redissonClient.getRateLimiter("testTryAcquireNoWait");
                RateLimiterConfig config = testBlockAcquire.getConfig();
                Assertions.assertEquals(1, config.getRate());
                Assertions.assertEquals(1000, config.getRateInterval());
                Assertions.assertEquals(RateType.OVERALL, config.getRateType());
                System.out.println(JSON.toJSONString(config));
            } catch (Exception e) {
                e.printStackTrace();
                result.set(false);
            }
        }

        @RedissonRateLimiter(
                name = "testTryAcquireWithWait",
                type = RedissonRateLimiter.Type.TRY,
                waitTime = 11,
                timeUnit = TimeUnit.SECONDS,
                rate = 1,
                rateInterval = 1,
                rateType = RateType.OVERALL,
                rateIntervalUnit = RateIntervalUnit.SECONDS
        )
        public void testTryAcquireWithWait() {
            try {
                list.add(System.currentTimeMillis());
                RRateLimiter testBlockAcquire = redissonClient.getRateLimiter("testTryAcquireWithWait");
                RateLimiterConfig config = testBlockAcquire.getConfig();
                Assertions.assertEquals(1, config.getRate());
                Assertions.assertEquals(1000, config.getRateInterval());
                Assertions.assertEquals(RateType.OVERALL, config.getRateType());
                System.out.println(JSON.toJSONString(config));
            } catch (Exception e) {
                e.printStackTrace();
                result.set(false);
            }
        }
    }

    @Autowired
    private TestRedissonRateLimiterClass testRedissonRateLimiterClass;

    @Test
    public void testBlockAcquire() throws InterruptedException {
        List<Long> list = testRedissonRateLimiterClass.getList();
        testRedissonRateLimiterClass.getResult().set(true);
        list.clear();
        Thread []threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
               testRedissonRateLimiterClass.testBlockAcquire();
            });
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].start();
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].join();
        }
        for (int i = 1; i < list.size(); i++) {
            Assertions.assertTrue(list.get(i) - list.get(i - 1) >= 1000);
        }
        Assertions.assertEquals(10, list.size());
        Assertions.assertTrue(testRedissonRateLimiterClass.getResult().get());
    }

    @Test
    public void testBlockAcquireWithParams() throws InterruptedException {
        List<Long> list = testRedissonRateLimiterClass.getList();
        testRedissonRateLimiterClass.getResult().set(true);
        list.clear();
        Thread []threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
               testRedissonRateLimiterClass.testBlockAcquireWithParams("test");
            });
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].start();
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].join();
        }
        for (int i = 1; i < list.size(); i++) {
            Assertions.assertTrue(list.get(i) - list.get(i - 1) >= 1000);
        }
        Assertions.assertEquals(10, list.size());
        Assertions.assertTrue(testRedissonRateLimiterClass.getResult().get());
    }

    @Test
    public void testTryAcquireNoWait() throws InterruptedException {
        List<Long> list = testRedissonRateLimiterClass.getList();
        testRedissonRateLimiterClass.getResult().set(true);
        list.clear();
        Thread []threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(9 * 1000));
                    testRedissonRateLimiterClass.testTryAcquireNoWait();
                } catch (Exception e) {
                    //ignore
                }
            });
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].start();
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].join();
        }
        for (int i = 1; i < list.size(); i++) {
            Assertions.assertTrue(list.get(i) - list.get(i - 1) >= 1000);
        }
        Assertions.assertTrue(list.size() < 10);
        Assertions.assertTrue(testRedissonRateLimiterClass.getResult().get());
    }

    @Test
    public void testTryAcquireWithWait() throws InterruptedException {
        List<Long> list = testRedissonRateLimiterClass.getList();
        testRedissonRateLimiterClass.getResult().set(true);
        list.clear();
        Thread []threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonRateLimiterClass.testTryAcquireWithWait();
                } catch (Exception e) {
                    //ignore
                }
            });
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].start();
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i].join();
        }
        for (int i = 1; i < list.size(); i++) {
            Assertions.assertTrue(list.get(i) - list.get(i - 1) >= 1000);
        }
        Assertions.assertEquals(10, list.size());
        Assertions.assertTrue(testRedissonRateLimiterClass.getResult().get());
    }
}
