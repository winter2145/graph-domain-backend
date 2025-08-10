package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.LikeTargetType;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.LikeRecordMapper;
import com.xin.graphdomainbackend.mapper.PictureMapper;
import com.xin.graphdomainbackend.model.dto.like.LikeQueryRequest;
import com.xin.graphdomainbackend.model.dto.like.LikeRequest;
import com.xin.graphdomainbackend.model.entity.Comments;
import com.xin.graphdomainbackend.model.entity.LikeRecord;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.model.vo.LikeRecordVO;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.model.vo.comment.CommentUserVO;
import com.xin.graphdomainbackend.model.vo.comment.CommentsVO;
import com.xin.graphdomainbackend.service.CommentsService;
import com.xin.graphdomainbackend.service.LikeRecordService;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【like_record】的数据库操作Service实现
* @createDate 2025-07-29 20:32:56
*/
@Service
@Slf4j
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord>
    implements LikeRecordService {

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserService userService;

    @Resource
    private CommentsService commentsService;

    @Override
    @Async("asyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Boolean> doLike(LikeRequest likeRequest, Long userId) {
        try {
            // 参数校验
            ThrowUtils.throwIf(likeRequest == null, ErrorCode.PARAMS_ERROR);

            Long targetId = likeRequest.getTargetId();
            Integer targetType = likeRequest.getTargetType();
            Boolean isLiked = likeRequest.getIsLiked();

            // 参数校验
            if (targetId == null || targetType == null || isLiked == null || userId == null) {
                log.error("Invalid parameters: targetId={}, targetType={}, isLiked={}, userId={}",
                        targetId, targetType, isLiked, userId);
                return CompletableFuture.completedFuture(false);
            }
            // 获取目标内容 所属用户ID
            Picture targetPicture = pictureService.getById(targetId);
            ThrowUtils.throwIf(targetPicture == null, ErrorCode.NOT_FOUND_ERROR);
            Long targetUserId = targetPicture.getUserId();

            Boolean success = dealDoLike(userId, targetId, targetType, isLiked, targetUserId);

            // 更新图片 的点赞数量
            if (success) {
                updateLikeCount(targetId, targetType, isLiked ? 1 : -1);
            }
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public Boolean dealDoLike(Long userId, Long targetId, Integer targetType, Boolean isLiked, Long targetUserId) {
        // 查询当前点赞状态
        LikeRecord oldLikeRecord = this.lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType)
                .one();

        boolean result = false;

        // 处理点赞记录
        if (oldLikeRecord == null) {
            // 首次操作
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(userId);
            likeRecord.setTargetId(targetId);
            likeRecord.setTargetType(targetType);
            likeRecord.setTargetUserId(targetUserId);
            likeRecord.setIsLiked(isLiked);
            likeRecord.setFirstLikeTime(new Date());
            likeRecord.setLastLikeTime(new Date());
            likeRecord.setIsRead(0); // 未读状态
            result = this.save(likeRecord);
        } else {
            // 更新现有记录
            if (!isLiked.equals(oldLikeRecord.getIsLiked())) {
                // 状态发生变化，更新记录
                oldLikeRecord.setIsLiked(isLiked);
                oldLikeRecord.setLastLikeTime(new Date());
                oldLikeRecord.setTargetUserId(targetUserId);

                // 如果是点赞操作，设置为未读
                if (isLiked) {
                    oldLikeRecord.setIsRead(0);
                }
                result = this.updateById(oldLikeRecord);
            } else {
                // 状态没有变化，可能是重复操作，返回true表示操作成功
                result = true;
            }
        }

        return result;
    }

    @Override
    public Page<LikeRecordVO> getUserLikeHistory(LikeQueryRequest likeQueryRequest, String source, Long userId) {
        ThrowUtils.throwIf(likeQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = likeQueryRequest.getCurrent();
        int pageSize = likeQueryRequest.getPageSize();

        // 创建分页对象
        Page<LikeRecord> page = new Page<>(current, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (source == MessageSourceEnum.TO_ME.getValue()) {
            lambdaQueryWrapper.eq(LikeRecord::getTargetUserId, userId) // 查询被点赞的记录
                    .eq(LikeRecord::getIsLiked, true)  // 只查询点赞状态为true的记录
                    .ne(LikeRecord::getUserId, userId); // 排除自己点赞自己的记录
        } else if (source == MessageSourceEnum.FROM_ME.getValue()) {
            lambdaQueryWrapper.eq(LikeRecord::getUserId, userId) // 查询我点赞的记录
                    .eq(LikeRecord::getIsLiked, true);
        }

        // 处理目标类型查询
        Integer targetType = likeQueryRequest.getTargetType();
        if (targetType != null) {
            lambdaQueryWrapper.eq(LikeRecord::getTargetType, targetType);
        }
        lambdaQueryWrapper.orderByDesc(LikeRecord::getLastLikeTime);

        // 执行分页查询
        Page<LikeRecord> likePage = this.page(page, lambdaQueryWrapper);

        // 转换VO结果
        List<LikeRecordVO> likeRecordVOS = convertToVOList(likePage.getRecords());

        // 构建返回结果
        Page<LikeRecordVO> voPage = new Page<>(likePage.getCurrent(), likePage.getSize(), likePage.getTotal());
        voPage.setRecords(likeRecordVOS);

        return voPage;

    }

    @Override
    public long getUnreadLikesCount(Long userId) {
        return this.count(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetUserId, userId)
                .eq(LikeRecord::getIsRead, 0)
                .eq(LikeRecord::getIsLiked, true)
                .ne(LikeRecord::getUserId, userId));
    }

    private List<LikeRecordVO> convertToVOList(List<LikeRecord> likeRecords) {
        if (CollUtil.isEmpty(likeRecords)) {
            return Collections.emptyList();
        }

        // 1. 批量查用户
        Set<Long> allUserIds = likeRecords.stream()
                .map(LikeRecord::getUserId)
                .collect(Collectors.toSet());

        // 同时收集图片作者 & 帖子作者
        List<Long> pictureIds = likeRecords.stream()
                .filter(like -> like.getTargetType() == 1)
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());

        List<Long> commentIds = likeRecords.stream()
                .filter(like -> like.getTargetType() == 4)
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());

        Map<Long, Picture> pictureMap = pictureIds.isEmpty()
                ? Collections.emptyMap()
                : pictureService.listByIds(pictureIds).stream()
                .peek(pic -> allUserIds.add(pic.getUserId()))
                .collect(Collectors.toMap(Picture::getId, Function.identity()));

        Map<Long, Comments> commentMap = commentIds.isEmpty()
                ? Collections.emptyMap()
                : commentsService.listByIds(commentIds).stream()
                .peek(post -> allUserIds.add(post.getUserId()))
                .collect(Collectors.toMap(Comments::getCommentId, Function.identity()));

        // 批量查所有相关用户
        Map<Long, UserVO> userVOMap = allUserIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 2. 组装 VO
        return likeRecords.stream().map(like -> {
            LikeRecordVO vo = new LikeRecordVO();
            BeanUtils.copyProperties(like, vo);

            // 点赞用户
            vo.setUser(userVOMap.get(like.getUserId()));

            // 目标内容
            switch (like.getTargetType()) {
                case LikeTargetType.IMAGE: // 图片
                    Picture picture = pictureMap.get(like.getTargetId());
                    if (picture != null) {
                        PictureVO pictureVO = PictureVO.objToVo(picture);
                        pictureVO.setUser(userVOMap.get(picture.getUserId()));
                        vo.setTarget(pictureVO);
                    }
                    break;
                case LikeTargetType.COMMENT: // 评论
                    Comments comments = commentMap.get(like.getTargetId());
                    if (comments != null) {
                        CommentsVO commentsVO = CommentsVO.objToVo(comments);
                        UserVO userVO = userVOMap.get(comments.getUserId());
                        CommentUserVO commentUserVO = UserVO.objToCommentUserVO(userVO);
                        commentsVO.setCommentUser(commentUserVO);
                        vo.setTarget(commentsVO);
                    }
                    break;
                default:
                    log.error("Unsupported target type: {}", like.getTargetType());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<LikeRecordVO> getAndClearUnreadLikes(Long userId) {
        // 获取未读点赞记录
        LambdaQueryWrapper<LikeRecord> likeRecordQueryWrapper = new LambdaQueryWrapper<>();
        likeRecordQueryWrapper.eq(LikeRecord::getTargetUserId, userId)
                .eq(LikeRecord::getIsRead, 0)
                .ne(LikeRecord::getUserId, userId)
                .in(LikeRecord::getTargetType,  Arrays.asList(
                        LikeTargetType.IMAGE,
                        LikeTargetType.POST,
                        LikeTargetType.SPACE,
                        LikeTargetType.COMMENT
                ))
                .orderByDesc(LikeRecord::getLastLikeTime)
                .last("LIMIT 50"); // 限制最多返回50条数据

        List<LikeRecord> unreadLikes = this.list(likeRecordQueryWrapper);
        if (CollUtil.isEmpty(unreadLikes)) {
            return new ArrayList<>();
        }

        // 批量更新为已读
        List<Long> likeIds = unreadLikes.stream()
                .map(LikeRecord::getId)
                .collect(Collectors.toList());

        this.lambdaUpdate().set(LikeRecord::getIsRead, 1)
                .in(LikeRecord::getId, likeIds)
                .update();

        // 返回之前未读的VO
        return convertToVOList(unreadLikes);
    }

    private void updateLikeCount(Long targetId, Integer targetType, int delta) {
        int updated = 0;
        if (targetType == LikeTargetType.IMAGE) {
            updated = pictureMapper.update(null,
                    Wrappers.<Picture>lambdaUpdate()
                            .setSql("likeCount = likeCount + " + delta)
                            .eq(Picture::getId, targetId)
                            .ge(Picture::getLikeCount, -delta)
            );
        }
        if (updated == 0) {
            log.warn("点赞数数更新失败，targetId: {}", targetId);
        }
    }


}