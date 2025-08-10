package com.xin.graphdomainbackend.model.dto.comments;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除评论 请求
 */
@Data
public class CommentsDeleteRequest implements Serializable {
    /**
     *  评论id
     */
    private Long commentId;

    private static final long serialVersionUID = 1L;
}
