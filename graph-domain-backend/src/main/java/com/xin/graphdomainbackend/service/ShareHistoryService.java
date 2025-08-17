package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.entity.ShareHistory;

import java.time.Duration;

/**
* @author Administrator
* @description 针对表【share_history(记录用户每一次真实的分享行为)】的数据库操作Service
* @createDate 2025-08-16 17:14:15
*/
public interface ShareHistoryService extends IService<ShareHistory> {
    /**
     * 检查用户在一定时间内分享次数是否超过限制
     *
     * @param userId     用户ID
     * @param targetId   内容ID
     * @param targetType 内容类型
     * @param duration   时间窗口
     * @param maxCount   最大次数
     * @return true=超过限制, false=未超过
     */
    boolean isOverShareLimit(Long userId, Long targetId, Integer targetType, Duration duration, int maxCount);

    /**
     * 记录一次分享历史
     *
     * @param userId       用户ID
     * @param targetId     内容ID
     * @param targetType   内容类型
     * @param targetUserId 内容作者ID
     * @return ShareHistory 对象
     */
    ShareHistory recordShareHistory(Long userId, Long targetId, Integer targetType, Long targetUserId);
}
