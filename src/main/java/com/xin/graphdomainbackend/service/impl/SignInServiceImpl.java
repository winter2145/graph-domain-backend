package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.UserSigninRecordMapper;
import com.xin.graphdomainbackend.model.entity.UserSigninRecord;
import com.xin.graphdomainbackend.service.PointsService;
import com.xin.graphdomainbackend.service.SignInService;
import com.xin.graphdomainbackend.utils.BitMapUtil;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SignInServiceImpl extends ServiceImpl<UserSigninRecordMapper, UserSigninRecord>
        implements SignInService {

    @Resource
    private UserSigninRecordMapper signInRecordMapper;

    @Resource
    private PointsService pointsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTemplate<String, byte[]> byteRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean signIn(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);

        int year = LocalDate.now().getYear();
        int dayOfYear = LocalDate.now().getDayOfYear(); // 返回 1~366

        // 1. Redis 键：user:signIn:xxx:2025  -> bit 位偏移 dayOfYear
        String cacheKey = RedisConstant.USER_SIGN_IN_REDIS_KEY_PREFIX + userId + ":" + year;

        // 2. 先看今天是否已签到（bit=1）
        //  Redis 的位偏移从 0 开始,则 dayOfYear - 1 对应 Redis 的 0 ~ 365
        //  所以查询 redis的偏移 0 对应 日期 1号
        Boolean todaySigned = stringRedisTemplate.opsForValue().getBit(cacheKey, dayOfYear - 1);
        if (Boolean.TRUE.equals(todaySigned)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
        }

        // 3. 创建新的签到记录对象插入
        try {
            UserSigninRecord newRecord = new UserSigninRecord();
            newRecord.setPoints(1);
            newRecord.setUserId(userId);
            newRecord.setSignDate(LocalDate.now());
            signInRecordMapper.insert(newRecord);

            // 4. 增加积分
            pointsService.addPoints(userId, 1, "每日签到");

            // 5. 设置redis缓存签到位, 真 - 代表签到
            stringRedisTemplate.opsForValue().setBit(cacheKey, dayOfYear - 1, true);
            // 过期时间写到次年 1 月 3 日，防止跨年遗留
            LocalDateTime expire = LocalDateTime.of(year + 1, 1, 3, 0, 0);
            stringRedisTemplate.expire(cacheKey, Duration.between(LocalDateTime.now(), expire));
        } catch (Exception e) {
            log.error("签到记录保存失败 :" + e.getMessage());
        }

        return true;
    }

/*    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        // 1. 确定年份
        int targetYear = (year == null) ? LocalDate.now().getYear() : year;

        // 2. 起止日期
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate   = LocalDate.of(targetYear, 12, 31);

        // 3. 查询数据库
        List<UserSigninRecord> dbList = lambdaQuery()
                .eq(UserSigninRecord::getUserId, userId)
                .ge(UserSigninRecord::getSignDate, startDate)
                .le(UserSigninRecord::getSignDate, endDate)
                .list();

        // 4. 转成 dayOfYear 并排序
        return dbList.stream()
                .map(r -> r.getSignDate().getDayOfYear())
                .sorted()
                .collect(Collectors.toList());
    }*/

    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        int targetYear = (year == null) ? LocalDate.now().getYear() : year;
        String cacheKey = RedisConstant.USER_SIGN_IN_REDIS_KEY_PREFIX + userId + ":" + targetYear;
        String emptyMarkerKey = cacheKey + ":empty";

        // 1. 先看空标记
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyMarkerKey))) {
            return Collections.emptyList();
        }

        // 2. Redis 命中：一次性取出整个 bitmap
        byte[] bitmap = byteRedisTemplate.opsForValue().get(cacheKey);
        List<Integer> days = new ArrayList<>();
        if (bitmap != null && bitmap.length > 0) {
            for (int i = 0; i < bitmap.length * 8; i++) {
                if (BitMapUtil.getBit(bitmap, i)) {   // 自己写的位工具
                    days.add(i + 1);                  // dayOfYear 从 1 开始
                }
            }
            return days;
        }

        // 3. Redis 未命中 -> 回源数据库
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        LocalDate end   = LocalDate.of(targetYear, 12, 31);
        List<UserSigninRecord> dbList = lambdaQuery()
                .eq(UserSigninRecord::getUserId, userId)
                .between(UserSigninRecord::getSignDate, start, end)
                .list();

        // 4. 回填 Redis + 空标记
        if (dbList.isEmpty()) {
            stringRedisTemplate.opsForValue().set(emptyMarkerKey, "1",
                    Duration.ofMinutes(10));   // 空值短期缓存
            return Collections.emptyList();
        }

        // 5. 把已签到日期写回 bitmap
        for (UserSigninRecord record : dbList) {
            int day = record.getSignDate().getDayOfYear();
            stringRedisTemplate.opsForValue().setBit(cacheKey, day - 1, true);
        }
        LocalDateTime expire = LocalDateTime.of(targetYear + 1, 1, 3, 0, 0);
        stringRedisTemplate.expire(cacheKey, Duration.between(LocalDateTime.now(), expire));

        // 6. 返回 dayOfYear 列表
        return dbList.stream()
                .map(r -> r.getSignDate().getDayOfYear())
                .sorted()
                .collect(Collectors.toList());
    }
}
