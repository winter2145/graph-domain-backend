package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.UserSigninRecordMapper;
import com.xin.graphdomainbackend.model.entity.UserSigninRecord;
import com.xin.graphdomainbackend.service.PointsService;
import com.xin.graphdomainbackend.service.SignInService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean signIn(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);

        // 检查用户是否签到
        LocalDate today = LocalDate.now();
        UserSigninRecord existingRecord  = signInRecordMapper.selectByUserIdAndDate(userId, today);
        if (existingRecord  != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
        }

        // 插入签到记录
        // 创建新的签到记录对象
        try {
            UserSigninRecord newRecord = new UserSigninRecord();
            newRecord.setPoints(1);
            newRecord.setUserId(userId);
            newRecord.setSignDate(today);
            signInRecordMapper.insert(newRecord);

            // 增加积分
            pointsService.addPoints(userId, 1, "每日签到");
        } catch (Exception e) {
            log.error("签到记录保存失败 :" + e.getMessage());
        }

        return true;
    }

    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        // 1. 确定年份
        int targetYear = (year == null) ? LocalDate.now().getYear() : year;

        // 2. 起止日期
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate   = LocalDate.of(targetYear, 12, 31);

        // 3. 查询数据库
        List<UserSigninRecord> list = lambdaQuery()
                .eq(UserSigninRecord::getUserId, userId)
                .ge(UserSigninRecord::getSignDate, startDate)
                .le(UserSigninRecord::getSignDate, endDate)
                .list();

        // 4. 转成 dayOfYear 并排序
        return list.stream()
                .map(r -> r.getSignDate().getDayOfYear())
                .sorted()
                .collect(Collectors.toList());
    }
}
