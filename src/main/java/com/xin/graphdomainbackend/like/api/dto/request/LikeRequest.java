package com.xin.graphdomainbackend.like.api.dto.request;

import lombok.Data;

/**
 * 点赞请求
 */
@Data
public class LikeRequest {
    /**
     * 目标内容ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片
     */
    private Integer targetType;

    /**
     * 是否点赞
     */
    private Integer isLiked;
}