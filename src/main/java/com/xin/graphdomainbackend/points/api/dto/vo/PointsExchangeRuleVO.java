package com.xin.graphdomainbackend.points.api.dto.vo;

import lombok.Data;

/**
 * 积分兑换规则视图
 */
@Data
public class PointsExchangeRuleVO {
    /**
     * id
     */
    private Long id;

    /**
     * 目标等级
     */
    private Integer toLevel;

    /**
     * 普通版/专业版/旗舰版
     */
    private String toLevelName;

    /**
     * 需要的积分
     */
    private Integer costPoints;

    /**
     * 是否可兑换
     */
    private Boolean canExchange;

}
