package com.xin.graphdomainbackend.user.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 用户签到记录表
 * @TableName user_signin_record
 */
@TableName(value ="user_signin_record")
@Data
public class UserSigninRecord implements Serializable {
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
     * 签到日期
     */
    private LocalDate signDate;

    /**
     * 签到获得积分
     */
    private Integer points;

    /**
     * 签到时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}