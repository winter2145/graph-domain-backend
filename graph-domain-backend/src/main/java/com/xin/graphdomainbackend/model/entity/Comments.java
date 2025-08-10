package com.xin.graphdomainbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName comments
 */
@TableName(value ="comments")
@Data
public class Comments implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long commentId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 评论目标所属用户ID
     */
    private Long targetUserId;

    /**
     * 
     */
    private String content;

    /**
     * 
     */
    private Date createTime;

    /**
     * 0表示顶级
     */
    private Long parentCommentId;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 
     */
    private Long likeCount;

    /**
     * 
     */
    private Long dislikeCount;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Integer isRead;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}