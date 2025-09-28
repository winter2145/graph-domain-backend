package com.xin.graphdomainbackend.share.api.dto.request;

import lombok.Data;

/**
 * 分享请求
 */
@Data
public class ShareRequest {
    /**
     * 目标内容ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片
     */
    private Integer targetType;

    /**
     * 是否分享
     */
    private Boolean isShared;
}
