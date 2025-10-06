package com.xin.graphdomainbackend.aidraw.api.dto.vo;

import lombok.Data;

/**
 * AI 生成图片 视图
 */
@Data
public class AiGenerateImageVO {

    /**
     * 类型 ：text/image
     */
    private String type;

    /**
     * 内容
     */
    private String content;

    /**
     * 图片地址
     */
    private String cosUrl;

    /**
     * 轮次ID
     */
    private Long roundId;

    /**
     * 说话角色
     */
    private String role;
}
