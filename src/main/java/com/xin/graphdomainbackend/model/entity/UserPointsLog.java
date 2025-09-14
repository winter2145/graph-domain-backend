package com.xin.graphdomainbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.xin.graphdomainbackend.model.enums.PointsChangeTypeEnum;
import com.xin.graphdomainbackend.model.vo.PointsLogVO;
import lombok.Data;

/**
 * 用户积分流水表
 * @TableName user_points_log
 */
@TableName(value ="user_points_log")
@Data
public class UserPointsLog implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 变动类型：1-签到 2-兑换 3-系统赠送 4-其他
     */
    private Integer changeType;

    /**
     * 积分变化值（正数表示增加，负数表示减少）
     */
    private Integer changePoints;

    /**
     * 变动前积分
     */
    private Integer beforePoints;

    /**
     * 变动后积分
     */
    private Integer afterPoints;

    /**
     * 业务关联 id（比如 spaceId、签到记录id）
     */
    private Long bizId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public static PointsLogVO objToVO(UserPointsLog entity) {
        PointsLogVO vo = new PointsLogVO();
        vo.setUserId(entity.getUserId());
        Integer changeType = entity.getChangeType();
        vo.setChangeTypeDesc(PointsChangeTypeEnum.getEnumByValue(changeType).getText());
        vo.setChangePoints(entity.getChangePoints());
        vo.setAvailablePoints(entity.getAfterPoints());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}