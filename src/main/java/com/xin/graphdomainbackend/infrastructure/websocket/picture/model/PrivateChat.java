package com.xin.graphdomainbackend.infrastructure.websocket.picture.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 私聊表
 * @TableName private_chat
 */
@TableName(value ="private_chat")
@Data
public class PrivateChat implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标用户id
     */
    private Long targetUserId;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后一条消息时间
     */
    private Date lastMessageTime;

    /**
     * 用户未读消息数
     */
    private Integer userUnreadCount;

    /**
     * 目标用户未读消息数
     */
    private Integer targetUserUnreadCount;

    /**
     * 用户自定义的私聊名称
     */
    private String userChatName;

    /**
     * 目标用户自定义的私聊名称
     */
    private String targetUserChatName;

    /**
     * 聊天类型：0-私信 1-好友(双向关注)
     */
    private Integer chatType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 用户是否删除：true 删
     */
    private boolean userDeleted;

    /**
     * 目标用户是否删除：true 删
     */
    private boolean targetUserDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}