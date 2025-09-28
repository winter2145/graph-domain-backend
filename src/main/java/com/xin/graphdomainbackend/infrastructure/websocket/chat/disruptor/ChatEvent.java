package com.xin.graphdomainbackend.infrastructure.websocket.chat.disruptor;

import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import com.xin.graphdomainbackend.user.dao.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 聊天事件
 */
@Data
public class ChatEvent {

    /**
     * 消息内容
     */
    private ChatMessage chatMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 目标ID(图片ID/空间ID/私聊ID)
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private Integer targetType;

    /**
     * 清空事件数据
     */
    public void clear() {
        this.chatMessage = null;
        this.session = null;
        this.user = null;
        this.targetId = null;
        this.targetType = null;
    }
}
