package com.xin.graphdomainbackend.manager.crawler;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 通用计数器（可用于实现频率统计、限流、封禁等等）
 * 基于 Redis 和 Lua 脚本实现时间窗口内的计数。
 */
@Slf4j
@Service
public class CounterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 默认统计一分钟内的计数。
     * 每次调用会使该 key 对应的计数器加 1，并返回当前值。
     *
     * @param key 计数器的 Redis 缓存键
     * @return 当前时间段内的累计计数值
     */
    public long incrAndGetCounter(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES);
    }

    /**
     * 增加并返回计数（使用指定时间间隔）
     * 每个时间段单独维护一个计数器，例如每分钟一个 key。
     *
     * @param key          计数器的 Redis 缓存键
     * @param interval  时间间隔（多少个单位）
     * @param unit     时间单位（支持秒、分、小时）
     * @return 当前时间段内的累计计数值
     */
    private long incrAndGetCounter(String key, int interval, TimeUnit unit) {
        return incrAndGetCounter(key, interval, unit, toSeconds(interval, unit));
    }

    /**
     * 增加并返回计数（自定义过期时间）
     * 每个时间段单独维护一个计数器，例如每分钟一个 key。
     *
     * @param key          计数器的 Redis 缓存键
     * @param timeInterval 时间间隔（多少个单位）
     * @param timeUnit     时间单位（支持秒、分、小时）
     * @param expireSeconds   Redis Key 的过期时间（秒）
     * @return 当前时间窗口内的计数值
     */
    private long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expireSeconds) {
        if (StrUtil.hasBlank(key)) {
            return 0;
        }
        // 构建带时间片段的 Redis Key
        String redisKey = buildTimeBaseKey(key, timeInterval, timeUnit);

        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        " return redis.call('incr', KEYS[1]); " +
                        " else " +
                        " redis.call('set', KEYS[1], 1);" +
                        " redis.call('expire', KEYS[1], ARGV[1]);" +
                        " return 1;" +
                        " end ";
        Object result = redissonClient.getScript(IntegerCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey),
                expireSeconds
        );
        return (long) result;
    }

    /**
     * 构建基于时间片段的 Redis Key， / interval确保不同时间段的计数隔离。
     *
     * @param baseKey  原始 Redis 键
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return 带时间片段的 Redis 键（例如：ip:login:12345）
     */
    private String buildTimeBaseKey(String baseKey, int interval, TimeUnit unit) {
        long timestamp = Instant.now().getEpochSecond();
        long timeFactor;
        switch(unit) {
            case SECONDS: timeFactor = timestamp / interval;
            break;
            case MINUTES: timeFactor = timestamp / 60 / interval;
            break;
            case HOURS: timeFactor = timestamp / 3600 / interval;
            break;
            default: throw new IllegalArgumentException("Unsupported TimeUnit: " + unit);
        }
        return baseKey + ":" + timeFactor;
    }

    /**
     * 将时间间隔转换为秒数。
     *
     * @param timeInterval 时间数值
     * @param timeUnit     时间单位
     * @return 该时间间隔对应的秒数
     */
    private long toSeconds(int timeInterval, TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS: return timeInterval;
            case MINUTES: return timeInterval * 60L;
            case HOURS: return timeInterval * 3600L;
            default: throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        }
    }
}
