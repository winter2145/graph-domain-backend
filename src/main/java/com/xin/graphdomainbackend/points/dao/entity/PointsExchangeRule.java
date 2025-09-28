package com.xin.graphdomainbackend.points.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分兑换规则表
 * @TableName points_exchange_rule
 */
@TableName(value ="points_exchange_rule")
@Data
public class PointsExchangeRule implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原等级（0-普通版 1-专业版 2-旗舰版）
     */
    private Integer fromLevel;

    /**
     * 目标等级
     */
    private Integer toLevel;

    /**
     * 需要的积分
     */
    private Integer costPoints;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}