package com.xin.graphdomainbackend.manager;

import com.xin.graphdomainbackend.manager.crawler.CounterManager;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CounterManagerIntegrationTest {

    @Autowired
    private CounterManager counterManager;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void testIncrAndGetCounter_integration() {
        String key = "test:counter:integration";

        // 清理旧数据
        redissonClient.getKeys().deleteByPattern(key + "*");

        // 连续增加三次
        long count1 = counterManager.incrAndGetCounter(key);
        long count2 = counterManager.incrAndGetCounter(key);
        long count3 = counterManager.incrAndGetCounter(key);

        System.out.println("Counts: " + count1 + ", " + count2 + ", " + count3);

        assertTrue(count1 >= 1);
        assertTrue(count2 > count1);
        assertTrue(count3 > count2);
    }

}