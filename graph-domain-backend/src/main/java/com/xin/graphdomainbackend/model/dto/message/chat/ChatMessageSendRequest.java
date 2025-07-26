package com.xin.graphdomainbackend.model.dto.message.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息请求类
 */
@Data
public class ChatMessageSendRequest implements Serializable {

    private static final long serialVersionUID = 2746226708566511033L;

    /**
     * 发送内容
     */
    private String content;

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
     * 回复者id（评论用）
     */
    private Long replyId;

    /**
     * 会话根消息 ID，用于组织消息树（比如评论主干）
     */
    private Long rootId;

    /**
     * 回复人（私人聊天用）
     */
    private Long receiverId;


}
