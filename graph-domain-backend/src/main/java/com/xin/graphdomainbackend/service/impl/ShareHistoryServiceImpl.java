package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.model.entity.ShareHistory;
import com.xin.graphdomainbackend.service.ShareHistoryService;
import com.xin.graphdomainbackend.mapper.ShareHistoryMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

/**
* @author Administrator
* @description 针对表【share_history(记录用户每一次真实的分享行为)】的数据库操作Service实现
* @createDate 2025-08-16 17:14:15
*/
@Service
public class ShareHistoryServiceImpl extends ServiceImpl<ShareHistoryMapper, ShareHistory>
    implements ShareHistoryService{

    @Override
    public boolean isOverShareLimit(Long userId, Long targetId, Integer targetType, Duration duration, int maxCount) {
        LocalDateTime since = LocalDateTime.now().minus(duration);

        long count = this.lambdaQuery()
                .eq(ShareHistory::getUserId, userId)
                .eq(ShareHistory::getTargetId, targetId)
                .eq(ShareHistory::getTargetType, targetType)
                .ge(ShareHistory::getShareTime, since)
                .count();

        return count >= maxCount;
    }

    @Override
    public ShareHistory recordShareHistory(Long userId, Long targetId, Integer targetType, Long targetUserId) {
        ShareHistory history = new ShareHistory();
        history.setUserId(userId);
        history.setTargetId(targetId);
        history.setTargetType(targetType);
        history.setTargetUserId(targetUserId);
        history.setShareTime(new Date());

        this.save(history);
        return history;
    }
}




