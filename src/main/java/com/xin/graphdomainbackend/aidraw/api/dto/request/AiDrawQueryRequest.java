package com.xin.graphdomainbackend.aidraw.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 绘图查询请求
 */
@Data
public class AiDrawQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID
     */
    private Long sessionId;

}
