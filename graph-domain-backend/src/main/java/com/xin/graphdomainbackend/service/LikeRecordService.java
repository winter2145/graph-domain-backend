package com.xin.graphdomainbackend.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.like.LikeQueryRequest;
import com.xin.graphdomainbackend.model.dto.like.LikeRequest;
import com.xin.graphdomainbackend.model.entity.LikeRecord;
import com.xin.graphdomainbackend.model.vo.LikeRecordVO;

import java.util.List;
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
    Boolean dealDoLike(Long userId, Long targetId, Integer targetType, Boolean isLiked, Long targetUserId);

    /**
     * 获取用户的点赞历史（分页）
     */
    Page<LikeRecordVO> getUserLikeHistory(LikeQueryRequest likeQueryRequest, String source, Long userId);

    /**
     * 获取用户未读点赞数
     */
    long getUnreadLikesCount(Long userId);
}
