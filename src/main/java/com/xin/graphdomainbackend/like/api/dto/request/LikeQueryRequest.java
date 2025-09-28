package com.xin.graphdomainbackend.like.api.dto.request;


import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 点赞评论请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LikeQueryRequest extends PageRequest implements Serializable {
    /**
     * 目标类型：1-图片 2-帖子 4-评论
     */
    private Integer targetType;


    private static final long serialVersionUID = 1L;
}
