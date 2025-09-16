package com.xin.graphdomainbackend.manager;

import com.xin.graphdomainbackend.manager.crawler.CounterManagerService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CounterManagerServiceIntegrationTest {

    @Autowired
    private CounterManagerService counterManagerService;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void testIncrAndGetCounter_integration() {
        String key = "test:counter:integration";

        // 清理旧数据
        redissonClient.getKeys().deleteByPattern(key + "*");

        // 连续增加三次
        long count1 = counterManagerService.incrAndGetCounter(key);
        long count2 = counterManagerService.incrAndGetCounter(key);
        long count3 = counterManagerService.incrAndGetCounter(key);

        System.out.println("Counts: " + count1 + ", " + count2 + ", " + count3);

        assertTrue(count1 >= 1);
        assertTrue(count2 > count1);
        assertTrue(count3 > count2);
    }

}