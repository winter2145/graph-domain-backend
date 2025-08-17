package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.comments.CommentsAddRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsDeleteRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsLikeRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsQueryRequest;
import com.xin.graphdomainbackend.model.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.model.vo.comment.CommentsVO;
import com.xin.graphdomainbackend.service.CommentsService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentsController {

    @Resource
    private CommentsService commentsService;

    // 添加评论
    @PostMapping("/add")
    @LoginCheck
    public BaseResponse<Boolean> addComment(@RequestBody CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
        Boolean result = commentsService.addComment(commentsAddRequest, request);
        return ResultUtils.success(result);
    }

    // 删除评论
    @PostMapping("/delete")
    @LoginCheck
    public BaseResponse<Boolean> deleteComment(@RequestBody CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.deleteComment(commentsDeleteRequest, request));
    }

    // 查询评论
    @PostMapping("/query")
    @LoginCheck
    public BaseResponse<Page<CommentsVO>> queryComment(@RequestBody CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.queryComment(commentsQueryRequest, request));
    }

    // 获取回复我的 历史
    @PostMapping("/commented/history")
    @LoginCheck
    public BaseResponse<Page<CommentsVO>> getCommentedHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<CommentsVO> commentHistory = commentsService.getCommentedHistory(commentsQueryRequest,
                MessageSourceEnum.TO_ME.getValue(), request);
        return ResultUtils.success(commentHistory);
    }

    // 获取我回复的 历史
    @PostMapping("/my/history")
    @LoginCheck
    public BaseResponse<Page<CommentsVO>> getMyCommentHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<CommentsVO> commentHistory = commentsService.getCommentedHistory(commentsQueryRequest,
                MessageSourceEnum.FROM_ME.getValue(), request);
        return ResultUtils.success(commentHistory);
    }

    /**
     * 获取未读评论列表
     * @param request HTTP请求
     * @return 未读评论列表
     */
    @GetMapping("/unread")
    @LoginCheck
    public BaseResponse<List<CommentsVO>> getUnreadComments(HttpServletRequest request) {

        List<CommentsVO> unreadComments = commentsService.getAndClearUnreadComments(request);
        return ResultUtils.success(unreadComments);
    }

    /**
     * 获取未读评论数量
     * @param request HTTP请求
     * @return 未读评论数量
     */
    @GetMapping("/unread/count")
    @LoginCheck
    public BaseResponse<Long> getUnreadCommentsCount(HttpServletRequest request) {

        return ResultUtils.success(commentsService.getUnreadCommentsCount(request));
    }

    /**
     * 点赞评论
     * @param commentslikeRequest 评论点赞请求
     * @param request HTTP请求
     * @return 点赞结果
     */
    @PostMapping("/like")
    @LoginCheck
    public BaseResponse<Boolean> likeComment(@RequestBody CommentsLikeRequest commentslikeRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.likeComment(commentslikeRequest, request));
    }


}
