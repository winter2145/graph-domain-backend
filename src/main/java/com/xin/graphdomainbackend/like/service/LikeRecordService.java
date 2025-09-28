package com.xin.graphdomainbackend.like.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.like.api.dto.request.LikeQueryRequest;
import com.xin.graphdomainbackend.like.api.dto.request.LikeRequest;
import com.xin.graphdomainbackend.like.api.dto.vo.LikeRecordVO;
import com.xin.graphdomainbackend.like.dao.entity.LikeRecord;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
* @author Administrator
* @description 针对表【like_record】的数据库操作Service
* @createDate 2025-07-29 20:32:56
*/
public interface LikeRecordService extends IService<LikeRecord> {

    /**
     * 通用点赞/取消点赞
     */
    CompletableFuture<Boolean> doLike(LikeRequest likeRequest, Long userId);

    /**
     * 获取并清除用户未读的点赞消息
     */
    List<LikeRecordVO> getAndClearUnreadLikes(Long userId);

    /**
     * 处理点赞逻辑
     */
    Boolean dealLikeOrDislike(Long userId, Long targetId, Integer targetType, Integer isLiked, Long targetUserId);

    /**
     * 获取用户的点赞历史（分页）
     */
    Page<LikeRecordVO> getUserLikeHistory(LikeQueryRequest likeQueryRequest, String source, Long userId);

    /**
     * 获取用户未读点赞数
     */
    long getUnreadLikesCount(Long userId);

    /**
     * 清除所有未读的点赞
     */
    void clearAllUnreadLikes(Long id);

    /**
     * 获取用户是否点赞
     */
    Boolean getIsLike(Long userId, Long targetId);

    /**
     * 根据targetIds 查找点赞记录
     */
    List<LikeRecord> getLikeRecordsByTargetIds(Set<Long> targetIds, Integer targetType);
}
