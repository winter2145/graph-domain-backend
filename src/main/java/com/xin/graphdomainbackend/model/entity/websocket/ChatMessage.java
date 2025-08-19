package com.xin.graphdomainbackend.model.entity.websocket;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天信息表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * 聊天Id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发送者Id
     */
    private Long senderId;

    /**
     * 接收者Id（在图片聊天室内可以不指定）
     */
    private Long receiverId;

    /**
     * 图片Id，对应图片聊天室
     */
    private Long pictureId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 状态 0 - 未读 1 - 已读
     */
    private Integer status;

    /**
     * 回复消息Id
     */
    private Long replyId;

    /**
     * 群聊或场景聊天室ID
     */
    private Long rootId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 私聊Id
     */
    private Long privateChatId;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 空间Id
     */
    private Long spaceId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}