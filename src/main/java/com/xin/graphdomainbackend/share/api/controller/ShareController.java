package com.xin.graphdomainbackend.share.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;

import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.share.api.dto.request.ShareQueryRequest;
import com.xin.graphdomainbackend.share.api.dto.request.ShareRequest;
import com.xin.graphdomainbackend.share.api.dto.vo.ShareRecordVO;
import com.xin.graphdomainbackend.share.service.ShareRecordService;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {

    @Resource
    private ShareRecordService shareRecordService;

    @Resource
    private UserService userService;

    /**
     * 通用分享接口
     */
    @PostMapping("/do")
    @LoginCheck
    public BaseResponse<Boolean> doShare(@RequestBody ShareRequest shareRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        try {
            CompletableFuture<Boolean> future = shareRecordService.doShare(shareRequest, loginUser.getId());
            Boolean result = future.get();
            return ResultUtils.success(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态，良好实践
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } catch (ExecutionException e) {
            // 拿到异步里的真实异常
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                throw (BusinessException) cause; // 交给 @RestControllerAdvice 处理
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行失败: " + cause.getMessage());
        }
    }

    /**
     * 获取未读分享消息
     */
    @GetMapping("/unread")
    @LoginCheck
    public BaseResponse<List<ShareRecordVO>> getUnreadShares(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        List<ShareRecordVO> unreadShares = shareRecordService.getAndClearUnreadShares(loginUser.getId());
        return ResultUtils.success(unreadShares);
    }

    /**
     * 获取别人分享我的历史
     */
    @PostMapping("/history")
    public BaseResponse<Page<ShareRecordVO>> getShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> likeHistory = shareRecordService.getUserShareHistory(shareQueryRequest, MessageSourceEnum.TO_ME.getValue(),loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    /**
     * 获取我点赞别人的历史
     */
    @PostMapping("/my/history")
    public BaseResponse<Page<ShareRecordVO>> getMyShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> likeHistory = shareRecordService.getUserShareHistory(shareQueryRequest, MessageSourceEnum.FROM_ME.getValue(),loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    /**
     * 获取未读的分享数
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadSharesCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(shareRecordService.getUnreadSharesCount(loginUser.getId()));
    }
}
