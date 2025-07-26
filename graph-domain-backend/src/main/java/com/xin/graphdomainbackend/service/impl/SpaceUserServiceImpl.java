package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.SpaceUserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.SpaceUserMapper;
import com.xin.graphdomainbackend.model.dto.spaceuser.*;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.SpaceRoleEnum;
import com.xin.graphdomainbackend.model.vo.SpaceUserVO;
import com.xin.graphdomainbackend.model.vo.SpaceVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Resource
    @Lazy
    private SpaceService spaceService;

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

        return users.stream()
                .map(user -> {
                    UserVO safetyUser = new UserVO();
                    BeanUtils.copyProperties(user, safetyUser);
                    return safetyUser;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSpaceMember(long userId, long spaceId) {
        LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SpaceUser::getUserId, userId)
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getStatus, 1); // 只有审核通过的才算是成员
        return this.count(lambdaQueryWrapper) > 0;
    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 1.校验参数
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);

        // 管理员，添加成员后，设置通过
        spaceUser.setStatus(1);
        validSpaceUser(spaceUser, true);

        // 2.校验空间成员数量是否达到上限
        boolean result = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceUserAddRequest.getSpaceId())
                .eq(SpaceUser::getStatus, 1) // 只统计已通过的成员
                .count() < 50;
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该空间成员数量已达到上限");
        }

        // 3.保存到数据库中
        boolean success = this.save(spaceUser);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        if (add) {
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceUserEditRequest.getId();

        // 查询空间内是否存在该用户
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);

        String targetRole = spaceUserEditRequest.getSpaceRole();
        spaceUser.setSpaceRole(targetRole);
        // 校验参数
        validSpaceUser(spaceUser, false);

        // 保存至数据库
        return this.updateById(spaceUser);
    }

    @Override
    public LambdaQueryWrapper<SpaceUser> getLamQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 获取参数
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        Integer status = spaceUserQueryRequest.getStatus();

        // 构建查询语句
        lambdaQueryWrapper.eq(ObjectUtils.isNotEmpty(id), SpaceUser::getId, id);
        lambdaQueryWrapper.eq(ObjectUtils.isNotEmpty(spaceId), SpaceUser::getSpaceId, spaceId);
        lambdaQueryWrapper.eq(ObjectUtils.isNotEmpty(userId), SpaceUser::getUserId, userId);
        lambdaQueryWrapper.eq(ObjectUtils.isNotEmpty(spaceRole), SpaceUser::getSpaceRole, spaceRole);
        lambdaQueryWrapper.eq(ObjectUtils.isNotEmpty(status), SpaceUser::getStatus, status);

        return lambdaQueryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);

        // 转换成封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        if (spaceUserVO == null) {
            return null;
        }

        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }

        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        // 设置状态
        spaceUserVO.setStatus(spaceUser.getStatus());
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {

        if (CollectionUtils.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        // 提前查询关联的用户Id与空间Id
        Set<Long> userIds = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        Set<Long> spaceIds = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());

        // 批量查询用户信息与空间信息，并转为Map
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds)
                .stream()
                .collect(
                        Collectors.toMap(User::getId, userService::getUserVO)
                );
        Map<Long, SpaceVO> spaceVOMap = spaceService.listByIds(spaceIds)
                .stream()
                .collect(
                        Collectors.toMap(Space::getId, spaceService::getSpaceVO)
                );

        // 转换记录
        return spaceUserList.stream()
                .map(spaceUser -> {
                    SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);

                    // 填充用户信息
                    Long userId = spaceUser.getUserId();
                    UserVO userVO = userVOMap.get(userId);
                    spaceUserVO.setUser(userVO);

                    // 填充空间信息
                    Long spaceId = spaceUser.getSpaceId();
                    SpaceVO spaceVO = spaceVOMap.get(spaceId);
                    spaceUserVO.setSpace(spaceVO);

                    return spaceUserVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean auditSpaceUser(SpaceUserAuditRequest spaceUserAuditRequest, User loginUser) {

        // 校验参数
        ThrowUtils.throwIf(spaceUserAuditRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserAuditRequest.getSpaceId();
        Long userId = spaceUserAuditRequest.getUserId();
        Integer status = spaceUserAuditRequest.getStatus();

        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId, status),
                ErrorCode.PARAMS_ERROR);
        if (status != SpaceUserConstant.PASS_STATUS && status != SpaceUserConstant.REFUSE_STATUS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不合法");
        }

        // 校验是否为管理员
        boolean isAdmin = lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, loginUser)
                .eq(SpaceUser::getSpaceRole, SpaceRoleEnum.ADMIN.getValue())
                .exists();
        ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR, "您不是该空间的管理员");

        // 查询目标用户记录
        SpaceUser targetUser = lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到该用户的申请记录");
        ThrowUtils.throwIf(SpaceRoleEnum.ADMIN.getValue()
                .equals(targetUser.getSpaceRole()), ErrorCode.OPERATION_ERROR, "不能审核管理员");

        // 若通过申请，校验人数上限
        if (status == SpaceUserConstant.PASS_STATUS) {
            long memberCount = lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getStatus, SpaceUserConstant.PASS_STATUS)
                    .count();
            ThrowUtils.throwIf(memberCount >= SpaceUserConstant.MAX_COUNT, ErrorCode.OPERATION_ERROR, "该空间成员数量已达到上限");
        }

        // 更新状态
        SpaceUser updateUser = new SpaceUser();
        updateUser.setId(targetUser.getId());
        updateUser.setStatus(status);
        return updateById(updateUser);
    }

    @Override
    public boolean joinSpace(SpaceUserJoinRequest spaceUserJoinRequest, User loginUser) {

        // 校验参数
        ThrowUtils.throwIf(spaceUserJoinRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        Long spaceId = spaceUserJoinRequest.getSpaceId();
        Long userId = loginUser.getId();

        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验用户是否已经是成员
        SpaceUser existSpaceUser = lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();

        if (existSpaceUser != null) {
            if (existSpaceUser.getStatus() == SpaceUserConstant.PASS_STATUS) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已是该空间成员");
            }
            if (existSpaceUser.getStatus() == SpaceUserConstant.EXAMINE_STATUS) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的申请正在审核中");
            }
        }

        // 创建申请记录
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(userId);
        spaceUser.setStatus(SpaceUserConstant.EXAMINE_STATUS);  // 设置为待审核状态
        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());  // 默认设置为查看者角色

        return this.save(spaceUser);
    }

}




