package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.entity.UserSigninRecord;

import java.util.List;

public interface SignInService extends IService<UserSigninRecord> {
    /**
     * 根据用户 id 签到
     * 未签，则签到
     * @return 是否签到
     */
    Boolean signIn(Long userId);

    /**
     * 获取用户某个年份的签到记录
     *
     * @param userId 用户 id
     * @param year   年份（为空表示当前年份）
     * @return 签到记录映射
     */
    List<Integer> getUserSignInRecord(long userId, Integer year);
}
