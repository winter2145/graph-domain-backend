package com.xin.graphdomainbackend.comments.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.comments.api.dto.request.CommentsAddRequest;
import com.xin.graphdomainbackend.comments.api.dto.request.CommentsDeleteRequest;
import com.xin.graphdomainbackend.comments.api.dto.request.CommentsLikeRequest;
import com.xin.graphdomainbackend.comments.api.dto.request.CommentsQueryRequest;
import com.xin.graphdomainbackend.comments.api.dto.vo.CommentsVO;
import com.xin.graphdomainbackend.comments.dao.entity.Comments;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;


/**
* @author Administrator
* @description 针对表【comments】的数据库操作Service
* @createDate 2025-07-26 14:10:43
*/
public interface CommentsService extends IService<Comments> {
    /**
     * 添加新的评论
     * @param commentsAddRequest 评论添加请求对象
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    Boolean addComment(CommentsAddRequest commentsAddRequest, HttpServletRequest request);

    /**
     * 删除评论
     * @param commentsDeleteRequest 评论删除请求对象
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    Boolean deleteComment(CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request);

    /**
     * 查询评论
     * @param commentsQueryRequest 评论查询请求
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    Page<CommentsVO> queryComment(CommentsQueryRequest commentsQueryRequest, HttpServletRequest request);

    /**
     * 获取 评论历史
     * @param commentsQueryRequest 评论查询请求
     * @param source 评论来源
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    Page<CommentsVO> getCommentedHistory(CommentsQueryRequest commentsQueryRequest, String source,
                                         HttpServletRequest request);

    /**
     * 获取用户未读评论数
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    long getUnreadCommentsCount(HttpServletRequest request);

    /**
     * 清除用户所有未读评论状态
     * @param userId 登录用户ID
     */
    void clearAllUnreadComments(Long userId);

    /**
     * 获取并清除用户未读的评论消息
     * @param request HTTP请求对象，用于获取当前用户信息
     */
    List<CommentsVO> getAndClearUnreadComments(HttpServletRequest request);

    /**
     * 点赞评论
     * @param commentsLikeRequest 评论点赞请求
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return
     */
    Boolean likeComment(CommentsLikeRequest commentsLikeRequest, HttpServletRequest request);
}
