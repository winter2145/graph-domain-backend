package com.xin.graphdomainbackend.share.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分享查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShareQueryRequest extends PageRequest implements Serializable {
    /**
     * 目标类型：1-图片
     */
    private Integer targetType;


    private static final long serialVersionUID = 1L;
}
