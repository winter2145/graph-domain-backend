package com.xin.graphdomainbackend.share.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 记录用户每一次真实的分享行为
 * @TableName share_history
 */
@TableName(value ="share_history")
@Data
public class ShareHistory implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 被分享内容的ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 
     */
    private Integer targetType;

    /**
     * 被分享内容所属用户ID
     */
    private Long targetUserId;

    /**
     * 分享时间
     */
    private Date shareTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}