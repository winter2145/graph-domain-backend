package com.xin.graphdomainbackend.infrastructure.websocket.chat.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息响应类
 */
@Data
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 8738715419317075084L;

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 发送内容
     */
    private String content;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 图片聊天室id
     */
    private Long pictureId;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 私人聊天id
     */
    private Long privateChatId;

    /**
     * 回复者id
     */
    private Long replyId;

    /**
     * 会话根消息 ID，用于组织消息树（比如评论主干）
     */
    private Long rootId;

    /**
     * 消息发送时间
     */
    private Date createTime;

    /**
     * 消息类型
     */
    private Integer type;
}
