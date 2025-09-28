package com.xin.graphdomainbackend.points.api.dto.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水 视图
 */
@Data
public class PointsLogVO {

    /**
     * 用户id
     **/
    private Long userId;

    /**
     * 类型描述（例如 "签到" / "兑换"/ "系统赠送"）
     **/
    private String changeTypeDesc;

    /**
     * 积分变化（正为增加，负为减少）
     **/
    private Integer changePoints;

    /**
     * 当前可用积分
     */
    private Integer availablePoints;

    /**
     * 备注
     */
    private String remark;

    /**
     * 发生时间
     */
    private LocalDateTime createTime;

}
