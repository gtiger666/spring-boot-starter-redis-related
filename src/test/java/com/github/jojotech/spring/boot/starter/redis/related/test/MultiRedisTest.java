package com.github.jojotech.spring.boot.starter.redis.related.test;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import com.github.jojotech.spring.boot.starter.redis.related.conf.RedissonAopConfiguration;
import com.github.jojotech.spring.boot.starter.redis.related.exception.RedisRelatedException;
import com.github.jojotech.spring.boot.starter.redis.related.lettuce.MultiRedisLettuceConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RLock;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "spring.redis.redisson.aop.order=" + MultiRedisTest.ORDER,
        "spring.redis.enable-multi=true",
        "spring.redis.multi.default.host=127.0.0.1",
        "spring.redis.multi.default.port=6379",
        "spring.redis.multi.test.host=127.0.0.1",
        "spring.redis.multi.test.port=6380",
        "logging.level.org.springframework.boot.autoconfigure=debug",
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
        @Autowired
        private RedissonClient redissonClient;

        @Bean
        public TestRedissonLockClass testRedissonLockClass() {
            return new TestRedissonLockClass(redissonClient);
        }
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

    public static final int ORDER = -100000;
    private static final int THREAD_COUNT = 100;
    private static final int ADD_COUNT = 10000;
    @Autowired
    private RedissonAopConfiguration redissonAopConfiguration;

    @Data
    public static class TestRedissonLockClass {
        private volatile int count = 0;
        private final RedissonClient redissonClient;

        public void reset() {
            count = 0;
        }

        private void add() throws InterruptedException {
            for (int i = 0; i < ADD_COUNT; i++) {
                count = count + 1;
            }
        }

        public void testNoLock() throws InterruptedException {
            add();
        }

        @RedissonLock(lockType = RedissonLock.BLOCK_LOCK)
        public void testBlockLock(@RedissonLockName String name) throws InterruptedException {
            add();
        }

        @RedissonLock(lockType = RedissonLock.BLOCK_LOCK, lockFeature = RedissonLock.LockFeature.SPIN)
        public void testBlockSpinLock(@RedissonLockName String name) throws InterruptedException {
            add();
        }

        @RedissonLock(lockType = RedissonLock.BLOCK_LOCK, lockFeature = RedissonLock.LockFeature.FAIR)
        public void testBlockFairLock(@RedissonLockName String name) throws InterruptedException {
            add();
        }

        @RedissonLock(lockType = RedissonLock.TRY_LOCK, waitTime = 10000, timeUnit = TimeUnit.MILLISECONDS)
        public void testTryLock(@RedissonLockName String name) throws InterruptedException {
            add();
        }

        @RedissonLock(lockType = RedissonLock.TRY_LOCK_NOWAIT)
        public void testTryLockNoWait(@RedissonLockName String name) throws InterruptedException {
            add();
            //3s 肯定够100个线程都 try lock 失败
            TimeUnit.SECONDS.sleep(3);
        }

        @RedissonLock
        public void testRedissonLockNameProperty(@RedissonLockName(prefix = "test:", expression = "#{id==null?name:id}") Student student, String params) throws InterruptedException {
            String lockName = student.getId() == null ? student.getName() : student.getId();
            RLock lock = redissonClient.getLock("test:" + lockName);
            Assertions.assertTrue(lock.isHeldByCurrentThread());
        }

        @RedissonLock(leaseTime = 1000L)
        public void testLockTime(@RedissonLockName String name) throws InterruptedException {
            RLock lock = redissonClient.getLock(RedissonLockName.DEFAULT_PREFIX + name);
            //验证获取了锁
            Assertions.assertTrue(lock.isHeldByCurrentThread());
            TimeUnit.SECONDS.sleep(2);
            //过了两秒，锁应该被释放了
            Assertions.assertFalse(lock.isLocked());
        }

        //waitTime只对于 trylock 有效
        @RedissonLock(lockType = RedissonLock.TRY_LOCK, waitTime = 1000L)
        public void testWaitTime(@RedissonLockName String name) throws InterruptedException {
            RLock lock = redissonClient.getLock(RedissonLockName.DEFAULT_PREFIX + name);
            //验证获取了锁
            Assertions.assertTrue(lock.isHeldByCurrentThread());
            TimeUnit.SECONDS.sleep(10);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Student {
        private String name;
        private String id;
        private int age;
    }

    @Autowired
    private TestRedissonLockClass testRedissonLockClass;

    @Test
    public void testAopConfiguration() {
        Assertions.assertEquals(redissonAopConfiguration.getOrder(), ORDER);
    }

    @Test
    public void testMultipleLock() throws InterruptedException {
        testRedissonLockClass.reset();
        //首先测无锁试多线程更新，这样最后的值肯定小于等于期望
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testNoLock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread item : threads) {
            item.join();
        }
        Assertions.assertTrue(testRedissonLockClass.getCount() < THREAD_COUNT * ADD_COUNT);
        //测试阻塞锁，最后的值应该等于期望值
        testRedissonLockClass.reset();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testBlockLock("same");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread value : threads) {
            value.join();
        }
        Assertions.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

        testRedissonLockClass.reset();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testBlockSpinLock("same");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread value : threads) {
            value.join();
        }
        Assertions.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

        testRedissonLockClass.reset();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testBlockFairLock("same");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread value : threads) {
            value.join();
        }
        Assertions.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

        //测试 tryLock锁 + 等待时间，由于是本地 redis 这个 10s 等待时间应该足够，最后的值应该等于期望值
        testRedissonLockClass.reset();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testTryLock("same");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Assertions.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);
        //测试 tryLock锁，不等待
        testRedissonLockClass.reset();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    testRedissonLockClass.testTryLockNoWait("same");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RedisRelatedException e) {
                    System.out.println(e.getMessage());
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        //由于锁住的时间比较久，只有一个线程执行了 add()
        Assertions.assertEquals(testRedissonLockClass.getCount(), ADD_COUNT);
    }

    @Test
    public void testBlockProperty() throws InterruptedException {
        testRedissonLockClass.reset();
        testRedissonLockClass.testRedissonLockNameProperty(Student.builder().name("zhx").build(), "zhx");
        testRedissonLockClass.testRedissonLockNameProperty(Student.builder().id("111111").build(), "zhx");
    }

    @Test
    public void testLockTime() throws InterruptedException {
        testRedissonLockClass.reset();
        testRedissonLockClass.testLockTime("same");
    }

    @Test
    public void testWaitTime() throws InterruptedException {
        testRedissonLockClass.reset();
        Thread thread = new Thread(() -> {
            try {
                testRedissonLockClass.testWaitTime("same");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        TimeUnit.SECONDS.sleep(3);
        //在等待时间内获取不到锁，抛异常
        assertThrows(RedisRelatedException.class, () -> testRedissonLockClass.testWaitTime("same"));
    }

    @Test
    public void testMultiNameLock() throws InterruptedException {
        testRedissonLockClass.reset();
        //首先测无锁试多线程更新，这样最后的值肯定小于等于期望
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < threads.length; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> {
                try {
                    //相当于没有锁住
                    testRedissonLockClass.testBlockLock(threads[finalI].getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        Assertions.assertTrue(testRedissonLockClass.getCount() < THREAD_COUNT * ADD_COUNT);
    }
}
