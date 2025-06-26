package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.UserMapper;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowersQueryRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsAddRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.UserFollows;
import com.xin.graphdomainbackend.model.vo.FollowersAndFansVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.UserFollowsService;
import com.xin.graphdomainbackend.mapper.UserFollowsMapper;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【user_follows】的数据库操作Service实现
* @createDate 2025-06-21 21:45:59
*/
@Service
public class UserFollowsServiceImpl extends ServiceImpl<UserFollowsMapper, UserFollows>
    implements UserFollowsService{

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Boolean addUserFollows(UserFollowsAddRequest userFollowsAddRequest) {
        ThrowUtils.throwIf(userFollowsAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long followerId = userFollowsAddRequest.getFollowerId();
        Long followingId = userFollowsAddRequest.getFollowingId();
        Integer followStatus = userFollowsAddRequest.getFollowStatus();

        // 1. 参数校验
        ThrowUtils.throwIf(followerId == null || followingId == null || followStatus == null,
                ErrorCode.PARAMS_ERROR, "参数不能为空");
        ThrowUtils.throwIf(followStatus != 0 && followStatus != 1,
                ErrorCode.PARAMS_ERROR, "关注状态只能是0或1");
        // 2. 查询现有关注关系
        LambdaQueryWrapper<UserFollows> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserFollows::getFollowerId, followerId)
                .eq(UserFollows::getFollowingId, followingId);
        UserFollows userFollows = this.getOne(lambdaQueryWrapper);

        // 开启本地事务回滚
        Boolean result = transactionTemplate.execute(status -> {
            try {
                // 3. 处理关注/取消关注逻辑
                if (followStatus == 1) {
                    // 关注
                    handleFollowAction(userFollows, userFollowsAddRequest);
                } else {
                    // 取消关注
                    handleUnFollowAction(userFollows, userFollowsAddRequest);
                }

                return true;
            } catch (Exception e) {
                // 发生异常时回滚事务
                status.setRollbackOnly();
                log.error("处理关注失败：{}", e);
                return false;
            }
        });
        return Boolean.TRUE.equals(result);
    }

    @Override
    public Boolean findIsFollow(UserFollowsIsFollowsRequest userFollowsIsFollowsRequest) {
        // 1. 参数非空校验（使用Optional）
        Long followerId = Optional
                .ofNullable(userFollowsIsFollowsRequest)
                .map(UserFollowsIsFollowsRequest::getFollowerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));

        Long followingId = Optional
                .ofNullable(userFollowsIsFollowsRequest)
                .map(UserFollowsIsFollowsRequest::getFollowingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));

        // 2. 数值有效性校验
        if (followerId <= 0 || followingId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须为正数");
        }

        // 3. 查询数据库是否关注
        boolean isFollow = false;
        isFollow = this.lambdaQuery()
                .eq(UserFollows::getFollowerId, followerId)
                .eq(UserFollows::getFollowingId, followingId)
                .eq(UserFollows::getFollowStatus, 1)
                .exists();

        return isFollow;
    }

    @Override
    public Page<UserVO> getFollowOrFanList(UserFollowersQueryRequest userFollowersQueryRequest) {
        // 1. 参数校验与初始化
        ThrowUtils.throwIf(userFollowersQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 查询关注关系分页数据
        Page<UserFollows> userFollowsPage = queryRelationPage(userFollowersQueryRequest);
        if (CollectionUtils.isEmpty(userFollowsPage.getRecords())) {
            return new Page<>(userFollowsPage.getCurrent(), userFollowsPage.getSize());
        }

        // 3. 获取目标用户ID列表
        List<Long> targetIds = extraTargetUserIds(userFollowsPage.getRecords(), userFollowersQueryRequest);
        if (CollectionUtils.isEmpty(targetIds)) {
            return new Page<>(userFollowsPage.getCurrent(), userFollowsPage.getSize());
        }

        // 4. 批量查询用户信息并转换VO
        return buildResultPage(userFollowsPage, targetIds);

    }

    @Override
    public FollowersAndFansVO getFollowAndFansCount(Long id) {
        long followCount = this.lambdaQuery()
                .eq(UserFollows::getFollowerId, id)
                .eq(UserFollows::getFollowStatus, 1)
                .count();

        long fansCount = this.lambdaQuery()
                .eq(UserFollows::getFollowingId, id)
                .eq(UserFollows::getFollowStatus, 1)
                .count();

        return new FollowersAndFansVO(fansCount, followCount);
    }

    @Override
    public List<Long> getFollowList(Long id) {
        List<UserFollows> followsList = this.lambdaQuery()
                .eq(UserFollows::getFollowerId, id)
                .eq(UserFollows::getFollowStatus, 1)
                .list();
        return followsList.stream()
                .map(UserFollows::getFollowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建最终分页结果
     *
     * @param userFollowsPage 关注关系分页数据
     * @param targetIds 目标用户ID列表（需保持原始分页顺序）
     * @return 组装好的分页结果，包含：
     *         - 转换后的UserVO列表
     *         - 原始分页信息（current/size/total）
     * @implNote 使用批量查询优化性能，并通过stream保持ID顺序
     */
    private Page<UserVO> buildResultPage(Page<UserFollows> userFollowsPage, List<Long> targetIds) {
        Map<Long, User> userMap = userMapper.selectByIds(targetIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 保持原顺序转换VO
        List<UserVO> userVOs = targetIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(userService::getUserVO)
                .collect(Collectors.toList());
        
        // 构建分页结果
        Page<UserVO> resultPage = new Page<>(userFollowsPage.getCurrent(), userFollowsPage.getSize());
        resultPage.setRecords(userVOs);
        resultPage.setTotal(userFollowsPage.getTotal());
        return resultPage;
    }

    /**
     * 从关注关系中提取目标用户ID列表
     *
     * @param records 关注关系记录集
     * @param request 原始查询请求（用于确定提取逻辑）
     * @return 目标用户ID列表，根据searchType决定提取：
     *         - 0: 提取followingId（关注列表）
     *         - 1: 提取followerId（粉丝列表）
     * @apiNote 自动过滤null值ID
     */
    private List<Long> extraTargetUserIds(List<UserFollows> records, UserFollowersQueryRequest request) {
        return records.stream()
                .map(record -> request.getSearchType() == 0 ?
                        record.getFollowingId() : record.getFollowerId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 查询关注关系分页数据
     *
     * @param request 查询参数
     * @return 分页的关注关系数据，条件包括：
     *         - 动态应用followerId/followingId条件
     *         - 只查询有效关注状态（followStatus=1）
     *         - 使用请求的分页参数
     */
    private Page<UserFollows> queryRelationPage(UserFollowersQueryRequest request) {
        return this.lambdaQuery()
                .eq(request.getFollowerId() != null, UserFollows::getFollowerId, request.getFollowerId())
                .eq(request.getFollowingId() != null, UserFollows::getFollowingId, request.getFollowingId())
                .eq(UserFollows::getFollowStatus, 1)
                .page(new Page<>(request.getCurrent(), request.getPageSize()));
    }

    /**
     * 关注他人，包括处理互关的情况
     */
    private void handleFollowAction(UserFollows userFollows, UserFollowsAddRequest userFollowsAddRequest) {

        Long followerId = userFollowsAddRequest.getFollowerId();
        Long followingId = userFollowsAddRequest.getFollowingId();
        // 从未关注过, 创建新纪录
        if (userFollows == null) {
            userFollows = new UserFollows(); // ✅ 创建新对象，避免空指针
            userFollows.setFollowerId(followerId);
            userFollows.setFollowingId(followingId);
            userFollows.setCreateTime(new Date());
            userFollows.setFollowStatus(1);
        } else { // 存在记录，则重新设置关注状态
            userFollows.setFollowStatus(1);
        }

        // 检查是否存在反向关注
        boolean isMutual = this.lambdaQuery()
                .eq(UserFollows::getFollowerId, followingId)
                .eq(UserFollows::getFollowingId, followerId)
                .eq(UserFollows::getFollowStatus, 1)
                .count() > 0;

        // 更新本人互关状态
        userFollows.setIsMutual(isMutual ? 1 : 0);
        userFollows.setEditTime(new Date());
        userFollows.setLastInteractionTime(new Date());

        // 持久化
        this.saveOrUpdate(userFollows);
        // 更新对方 -> 互关
        if (isMutual) {
            this.lambdaUpdate().set(UserFollows::getIsMutual, 1)
                    .eq(UserFollows::getFollowerId, followingId)
                    .eq(UserFollows::getFollowingId, followerId)
                    .eq(UserFollows::getFollowStatus, 1)
                    .update();
        }
    }

    /**
     * 取消关注
     */
    private void handleUnFollowAction(UserFollows userFollows, UserFollowsAddRequest userFollowsAddRequest) {
        if (userFollows == null || userFollowsAddRequest == null) {
            return; // 或者抛出异常
        }

        Long followerId = userFollowsAddRequest.getFollowerId();
        Long followingId = userFollowsAddRequest.getFollowingId();

        // 先更新对方信息
        // 只有在互相关注时才更新对方
        if (userFollows.getIsMutual() == 1) {
            this.lambdaUpdate()
                    .set(UserFollows::getIsMutual, 0)
                    .eq(UserFollows::getFollowerId, followingId)
                    .eq(UserFollows::getFollowingId, followerId)
                    .eq(UserFollows::getFollowStatus, 1)
                    .update();
        }

        // 修改当前用户关注状态
        userFollows.setFollowStatus(0);
        userFollows.setIsMutual(0);
        userFollows.setEditTime(new Date());
        userFollows.setLastInteractionTime(new Date());

        // ✅ 持久化修改
        this.updateById(userFollows);
    }
}




