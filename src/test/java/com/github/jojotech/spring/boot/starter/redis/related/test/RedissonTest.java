package com.github.jojotech.spring.boot.starter.redis.related.test;

import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLock;
import com.github.jojotech.spring.boot.starter.redis.related.annotation.RedissonLockName;
import com.github.jojotech.spring.boot.starter.redis.related.conf.RedissonAopConfiguration;
import com.github.jojotech.spring.boot.starter.redis.related.exception.RedisRelatedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "spring.redis.redisson.aop.order=" + RedissonTest.ORDER,
})
public class RedissonTest {
    public static final int ORDER = -100000;
    private static final int THREAD_COUNT = 100;
    private static final int ADD_COUNT = 10000;
    @Autowired
    private RedissonAopConfiguration redissonAopConfiguration;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private RedisServer redisServer;


    public RedissonTest() throws IOException {
        this.redisServer = RedisServer.builder().port(6379).setting("maxheap 20m").build();
        redisServer.start();
    }

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
            Assert.assertTrue(lock.isHeldByCurrentThread());
        }

        @RedissonLock(leaseTime = 1000L)
        public void testLockTime(@RedissonLockName String name) throws InterruptedException {
            RLock lock = redissonClient.getLock(RedissonLockName.DEFAULT_PREFIX + name);
            //验证获取了锁
            Assert.assertTrue(lock.isHeldByCurrentThread());
            TimeUnit.SECONDS.sleep(2);
            //过了两秒，锁应该被释放了
            Assert.assertFalse(lock.isLocked());
        }

        //waitTime只对于 trylock 有效
        @RedissonLock(lockType = RedissonLock.TRY_LOCK, waitTime = 1000L)
        public void testWaitTime(@RedissonLockName String name) throws InterruptedException {
            RLock lock = redissonClient.getLock(RedissonLockName.DEFAULT_PREFIX + name);
            //验证获取了锁
            Assert.assertTrue(lock.isHeldByCurrentThread());
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

    @After
    public void after() {
        redisServer.stop();
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

    @Test
    public void testAopConfiguration() {
        Assert.assertEquals(redissonAopConfiguration.getOrder(), ORDER);
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
        Assert.assertTrue(testRedissonLockClass.getCount() < THREAD_COUNT * ADD_COUNT);
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
        Assert.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

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
        Assert.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

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
        Assert.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);

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
        Assert.assertEquals(testRedissonLockClass.getCount(), THREAD_COUNT * ADD_COUNT);
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
        Assert.assertEquals(testRedissonLockClass.getCount(), ADD_COUNT);
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

    @Test(expected = RedisRelatedException.class)
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
        testRedissonLockClass.testWaitTime("same");
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
        Assert.assertTrue(testRedissonLockClass.getCount() < THREAD_COUNT * ADD_COUNT);
    }
}
