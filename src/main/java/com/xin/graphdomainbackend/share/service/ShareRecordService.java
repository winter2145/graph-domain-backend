package com.xin.graphdomainbackend.share.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.share.api.dto.request.ShareQueryRequest;
import com.xin.graphdomainbackend.share.api.dto.request.ShareRequest;
import com.xin.graphdomainbackend.share.api.dto.vo.ShareRecordVO;
import com.xin.graphdomainbackend.share.dao.entity.ShareRecord;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
* @author Administrator
* @description 针对表【share_record(分享记录表)】的数据库操作Service
* @createDate 2025-08-11 18:18:30
*/
public interface ShareRecordService extends IService<ShareRecord> {
    /**
     * 通用分享/取消分享
     */
    CompletableFuture<Boolean> doShare(ShareRequest shareRequest, Long userId);

    /**
     * 获取并清除用户未读的分享消息
     */
    List<ShareRecordVO> getAndClearUnreadShares(Long userId);

    /**
     * 获取用户的分享历史
     */
    Page<ShareRecordVO> getUserShareHistory(ShareQueryRequest shareQueryRequest, String source, Long userId);

    /**
     * 获取用户未读分享数
     */
    long getUnreadSharesCount(Long userId);

    /**
     *清除所有未读的分享
     */
    void clearAllUnreadShares(Long id);

}
