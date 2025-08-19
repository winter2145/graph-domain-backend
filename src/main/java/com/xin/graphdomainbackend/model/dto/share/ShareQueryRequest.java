package com.xin.graphdomainbackend.model.dto.share;

import com.xin.graphdomainbackend.model.dto.PageRequest;
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
