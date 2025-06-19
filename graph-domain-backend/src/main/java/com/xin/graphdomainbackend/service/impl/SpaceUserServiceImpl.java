package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.SpaceUserMapper;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.SpaceUserService;

import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-06-15 10:20:45
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService {

    @Resource
    private UserService userService;

    @Override
    public List<UserVO> getAllSpaceMembers(Long spaceId) {
        // 1.校验参数
        ThrowUtils.throwIf(spaceId <= 0, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper.eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getStatus, 1);  // 只获取审核通过的成员
        // 查数据库
        List<SpaceUser> spaceUsers = this.list(lambdaQueryWrapper);
        if (CollUtil.isEmpty(spaceUsers)) {
            return Collections.emptyList();
        }

        // 获取所有用户ID
        Set<Long> userIds = spaceUsers.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        // 批量查询用户信息
        List<User> users = userService.listByIds(userIds);

        List<UserVO> userVOList = users.stream()
                .map(user -> {
                    UserVO safetyUser = new UserVO();
                    BeanUtils.copyProperties(user, safetyUser);
                    return safetyUser;
                })
                .collect(Collectors.toList());

        return userVOList;
    }

    @Override
    public boolean isSpaceMember(long userId, long spaceId) {
        LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getStatus, 1); // 只有审核通过的才算是成员
        return this.count(lambdaQueryWrapper) > 0;
    }

}




