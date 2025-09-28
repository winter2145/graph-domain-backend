package com.xin.graphdomainbackend.points.api.dto.vo;

import lombok.Data;

/**
 * 积分 视图
 */
@Data
public class PointsInfoVO {

    // 用户id
    private Long userId;

    // 当前可用积分
    private Integer availablePoints;

    // 累计积分（历史总和）
    private Integer totalPoints;
}
