package com.xin.graphdomainbackend.model.dto.message.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天历史请求类
 */
@Data
public class ChatHistoryMessageRequest implements Serializable {

    private static final long serialVersionUID = 6375003945520818755L;

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
     * 当前页号 1
     */
    private Long current = 1L;

    /**
     * 消息数量 20条
     */
    private Long size = 20L;
}
