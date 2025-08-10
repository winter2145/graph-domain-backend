package com.xin.graphdomainbackend.model.dto.comments;

import lombok.Data;

import java.io.Serializable;

/**
 * 增加评论 请求
 */
@Data
public class CommentsAddRequest implements Serializable {

    /**
     *  用户id
     */
    private Long userId;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子，默认为1(图片)
     */
    private Integer targetType = 1;

    /**
     *内容
     */
    private String content;

    /**
     *父类
     */
    private Long parentCommentId;
}
