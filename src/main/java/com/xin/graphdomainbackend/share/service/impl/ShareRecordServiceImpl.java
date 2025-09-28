package com.xin.graphdomainbackend.share.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xin.graphdomainbackend.common.constant.TargetTypeConstant;
import com.xin.graphdomainbackend.common.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.picture.api.dto.vo.PictureVO;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.picture.dao.mapper.PictureMapper;
import com.xin.graphdomainbackend.picture.service.PictureService;
import com.xin.graphdomainbackend.share.api.dto.request.ShareQueryRequest;
import com.xin.graphdomainbackend.share.api.dto.request.ShareRequest;
import com.xin.graphdomainbackend.share.api.dto.vo.ShareRecordVO;
import com.xin.graphdomainbackend.share.dao.entity.ShareRecord;
import com.xin.graphdomainbackend.share.dao.mapper.ShareRecordMapper;
import com.xin.graphdomainbackend.share.service.ShareHistoryService;
import com.xin.graphdomainbackend.share.service.ShareRecordService;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【share_record(分享记录表)】的数据库操作Service实现
* @createDate 2025-08-11 18:18:30
*/
@Service
@Slf4j
public class ShareRecordServiceImpl extends ServiceImpl<ShareRecordMapper, ShareRecord>
    implements ShareRecordService {

    @Resource
    private PictureService pictureService;

    @Resource
    private ShareHistoryService shareHistoryService;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserService userService;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Boolean> doShare(ShareRequest shareRequest, Long userId) {
        try {
            // 参数校验
            ThrowUtils.throwIf(shareRequest == null, ErrorCode.PARAMS_ERROR);

            Long targetId = shareRequest.getTargetId();
            Integer targetType = shareRequest.getTargetType();
            Boolean isShared = shareRequest.getIsShared();

            if (targetId == null || targetType == null || isShared == null || userId == null) {
                log.error("Invalid parameters: targetId={}, targetType={}, isShared={}, userId={}",
                        targetId, targetType, isShared, userId);
                return CompletableFuture.completedFuture(false);
            }

            // 获取目标内容 所属用户ID
            Picture targetPicture = pictureService.getById(targetId);
            ThrowUtils.throwIf(targetPicture == null, ErrorCode.NOT_FOUND_ERROR);
            Long targetUserId = targetPicture.getUserId();

            // 处理分享
            Boolean success = dealDoShare(userId, targetId, targetType, targetUserId);

            // 更新图片 的分享数量
            if (success) {
                updateShareCount(targetId, targetType, isShared ? 1: -1);
            }
            return CompletableFuture.completedFuture(true);
        } catch (BusinessException e) {
            // 不要吃掉，直接往外抛，交给全局异常处理
            throw e;
        } catch (Exception e) {
            log.error("分享失败：{}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // 更新图片表中分享数量
    private void updateShareCount(Long targetId, Integer targetType, int delta) {
        int updated = 0;
        if (targetType == TargetTypeConstant.IMAGE) {
            updated = pictureMapper.updateShareCount(targetId, delta);
        }
        if (updated == 0) {
            log.warn("分享数 更新失败，targetId: {}", targetId);
        }
    }

    // 处理分享
    private Boolean dealDoShare(Long userId, Long targetId, Integer targetType, Long targetUserId) {
        // 1. 频率限制：1小时最多3次
        if (shareHistoryService.isOverShareLimit(userId, targetId, targetType, Duration.ofHours(1), 3)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户在1小时内该内容分享超过3次，拒绝");
        }

        // 2. 插入历史
        shareHistoryService.recordShareHistory(userId, targetId, targetType, targetUserId);

        // 3. 更新/插入状态表
        ShareRecord oldRecord = this.lambdaQuery()
                .eq(ShareRecord::getUserId, userId)
                .eq(ShareRecord::getTargetId, targetId)
                .eq(ShareRecord::getTargetType, targetType)
                .one();

        if (oldRecord == null) { // 首次分享
            ShareRecord record = new ShareRecord();
            record.setUserId(userId);
            record.setTargetId(targetId);
            record.setTargetUserId(targetUserId);
            record.setTargetType(targetType);
            record.setIsShared(true);
            record.setShareTime(new Date());
            record.setIsRead(0);
            this.save(record);
        } else { // 再次分享
            oldRecord.setIsShared(true);
            oldRecord.setShareTime(new Date());
            oldRecord.setIsRead(0);
            this.updateById(oldRecord);
        }

        return true;
    }

    @Override
    public List<ShareRecordVO> getAndClearUnreadShares(Long userId) {
        // 获取未读点赞记录
        LambdaQueryWrapper<ShareRecord> shareRecordQueryWrapper = new LambdaQueryWrapper<>();
        shareRecordQueryWrapper.eq(ShareRecord::getTargetUserId, userId)
                .eq(ShareRecord::getIsRead, 0)
                .ne(ShareRecord::getUserId, userId)
                .in(ShareRecord::getTargetType, Arrays.asList(
                        TargetTypeConstant.IMAGE,
                        TargetTypeConstant.POST,
                        TargetTypeConstant.SPACE,
                        TargetTypeConstant.COMMENT
                ))
                .orderByDesc(ShareRecord::getShareTime)
                .last("LIMIT 50"); // 最多返回50条数据

        List<ShareRecord> unreadShares = this.list(shareRecordQueryWrapper);
        if (CollUtil.isEmpty(unreadShares)) {
            return new ArrayList<>();
        }

        // 批量更新为已读
        List<Long> shareIds = unreadShares.stream()
                .map(ShareRecord::getId)
                .collect(Collectors.toList());

        this.lambdaUpdate().set(ShareRecord::getIsRead, 1)
                .in(ShareRecord::getId, shareIds)
                .update();

        // 返回之前未读的VO
        return convertToVOList(unreadShares);
    }

    private List<ShareRecordVO> convertToVOList(List<ShareRecord> shareRecords) {
        if (CollUtil.isEmpty(shareRecords)) {
            return Collections.emptyList();
        }

        // 1. 批量查用户
        Set<Long> allUserIds = shareRecords.stream()
                .map(ShareRecord::getUserId)
                .collect(Collectors.toSet());

        // 收集图片作者 & 信息
        List<Long> pictureIds = shareRecords.stream()
                .filter(share -> share.getTargetType() == 1)
                .map(ShareRecord::getTargetId)
                .collect(Collectors.toList());

        Map<Long, Picture> pictureMap = pictureIds.isEmpty()
                ? Collections.emptyMap()
                : pictureService.listByIds(pictureIds).stream()
                .peek(pic -> allUserIds.add(pic.getUserId()))
                .collect(Collectors.toMap(Picture::getId, Function.identity()));

        // 批量查所有相关用户
        Map<Long, UserVO> userVOMap = allUserIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 2. 组装 VO
        return shareRecords.stream().map(share -> {
            ShareRecordVO vo = new ShareRecordVO();
            BeanUtils.copyProperties(share, vo);

            // 分享用户
            vo.setUser(userVOMap.get(share.getUserId()));

            // 目标内容
            switch (share.getTargetType()) {
                case TargetTypeConstant.IMAGE: // 图片
                    Picture picture = pictureMap.get(share.getTargetId());
                    if (picture != null) {
                        PictureVO pictureVO = PictureVO.objToVo(picture);
                        pictureVO.setUser(userVOMap.get(picture.getUserId()));
                        vo.setTarget(pictureVO);
                    }
                    break;
                case TargetTypeConstant.COMMENT: // 评论TODO
                    break;
                default:
                    log.error("Unsupported target type: {}", share.getTargetType());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<ShareRecordVO> getUserShareHistory(ShareQueryRequest shareQueryRequest, String source, Long userId) {
        ThrowUtils.throwIf(shareQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = shareQueryRequest.getCurrent();
        int pageSize = shareQueryRequest.getPageSize();

        // 创建分页对象
        Page<ShareRecord> page = new Page<>(current, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<ShareRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (source == MessageSourceEnum.TO_ME.getValue()) {
            lambdaQueryWrapper.eq(ShareRecord::getTargetUserId, userId) // 查询被分享的记录
                    .eq(ShareRecord::getIsShared, true)  // 只查询分享状态为true的记录
                    .ne(ShareRecord::getUserId, userId); // 排除自己分享自己的记录
        } else if (source == MessageSourceEnum.FROM_ME.getValue()) {
            lambdaQueryWrapper.eq(ShareRecord::getUserId, userId) // 查询我分享的记录
                    .eq(ShareRecord::getIsShared, true);
        }

        // 处理目标类型查询
        Integer targetType = shareQueryRequest.getTargetType();
        if (targetType != null) {
            lambdaQueryWrapper.eq(ShareRecord::getTargetType, targetType);
        }
        lambdaQueryWrapper.orderByDesc(ShareRecord::getShareTime);

        // 执行分页查询
        Page<ShareRecord> sharePage = this.page(page, lambdaQueryWrapper);

        // 转换VO结果
        List<ShareRecordVO> shareRecordVOS = convertToVOList(sharePage.getRecords());

        // 构建返回结果
        Page<ShareRecordVO> voPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        voPage.setRecords(shareRecordVOS);

        return voPage;
    }


    @Override
    public long getUnreadSharesCount(Long userId) {
        return this.count(new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getTargetUserId, userId)
                .eq(ShareRecord::getIsShared, true)
                .eq(ShareRecord::getIsRead, 0)
                .ne(ShareRecord::getUserId, userId)
        );
    }

    @Override
    public void clearAllUnreadShares(Long userId) {
        this.lambdaUpdate().set(ShareRecord::getIsRead, 1)
                .eq(ShareRecord::getIsRead, 0)
                .eq(ShareRecord::getIsShared, true)
                .eq(ShareRecord::getTargetUserId, userId)
                .update();
    }
}




