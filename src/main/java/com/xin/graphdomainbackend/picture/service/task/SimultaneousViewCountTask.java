package com.xin.graphdomainbackend.picture.service.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.picture.dao.mapper.PictureMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务：定期将 Redis 中的图片浏览量刷新到数据库
 * <p>
 * 1. 避免高并发下频繁更新数据库，提升性能
 * 2. 确保浏览量数据最终一致性
 */
@Slf4j
@Component
public class SimultaneousViewCountTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PictureMapper pictureMapper;

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void flushViewCountsToDb() {
        try {
            Set<String> keys = stringRedisTemplate.keys("picture:viewCount:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (String key : keys) {
                try {
                    String val = stringRedisTemplate.opsForValue().get(key);
                    if (val == null) continue;

                    long count = Long.parseLong(val);
                    if (count <= 0) continue;

                    Long pictureId = Long.valueOf(key.split(":")[2]);

                    log.debug("刷库图片 {} 浏览量: {}", pictureId, count);

                    // 使用事务确保数据一致性
                    UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.setSql("viewCount = viewCount + " + count);
                    updateWrapper.eq("id", pictureId);
                    boolean success = pictureMapper.update(null, updateWrapper) > 0;

                    if (success) {
                        // 刷库成功后，减少 Redis 中的计数
                        Long remaining = stringRedisTemplate.opsForValue().decrement(key, count);

                        // 如果 Redis 中计数为 0 或负数，删除 key
                        if (remaining != null && remaining <= 0) {
                            stringRedisTemplate.delete(key);
                        } else {
                            // 刷新过期时间
                            stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
                        }

                        log.debug("图片 {} 浏览量刷库成功，剩余 Redis 计数: {}", pictureId, remaining);
                    }
                } catch (Exception e) {
                    log.error("刷库图片浏览量失败，key: {}, error: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("刷库浏览量任务执行失败: {}", e.getMessage());
        }
    }
}
