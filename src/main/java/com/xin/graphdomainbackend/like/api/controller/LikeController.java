package com.xin.graphdomainbackend.like.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.like.api.dto.request.LikeQueryRequest;
import com.xin.graphdomainbackend.like.api.dto.request.LikeRequest;
import com.xin.graphdomainbackend.like.api.dto.vo.LikeRecordVO;
import com.xin.graphdomainbackend.like.service.LikeRecordService;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/like")
@Slf4j
public class LikeController {

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserService userService;

    /**
     * 通用点赞、取消 接口
     */
    @PostMapping("/do")
    @LoginCheck
    public BaseResponse<Boolean> doLike(@RequestBody LikeRequest likeRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        try {
            likeRecordService.doLike(likeRequest, loginUser.getId());
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("点赞失败，原因：{}", e.getMessage());
            return ResultUtils.success(false);
        }
    }

    /**
     * 获取未读点赞消息
     */
    @GetMapping("/unread")
    @LoginCheck
    public BaseResponse<List<LikeRecordVO>> getUnreadLikes(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        List<LikeRecordVO> unreadLikes = likeRecordService.getAndClearUnreadLikes(loginUser.getId());
        return ResultUtils.success(unreadLikes);
    }

    /**
     * 获取别人点赞我的历史
     */
    @PostMapping("/history")
    public BaseResponse<Page<LikeRecordVO>> getLikeHistory(@RequestBody LikeQueryRequest likeQueryRequest,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = likeQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<LikeRecordVO> likeHistory = likeRecordService.getUserLikeHistory(likeQueryRequest, MessageSourceEnum.TO_ME.getValue(),loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    /**
     * 获取我点赞别人的历史
     */
    @PostMapping("/my/history")
    public BaseResponse<Page<LikeRecordVO>> getMyLikeHistory(@RequestBody LikeQueryRequest likeQueryRequest,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = likeQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<LikeRecordVO> likeHistory = likeRecordService.getUserLikeHistory(likeQueryRequest, MessageSourceEnum.FROM_ME.getValue(),loginUser.getId());
        return ResultUtils.success(likeHistory);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadLikesCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(likeRecordService.getUnreadLikesCount(loginUser.getId()));
    }

}
