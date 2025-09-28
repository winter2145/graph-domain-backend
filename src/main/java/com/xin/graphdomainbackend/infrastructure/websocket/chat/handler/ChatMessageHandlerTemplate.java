package com.xin.graphdomainbackend.infrastructure.websocket.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;


/**
 * 聊天消息 处理模板
 */
@Slf4j
public abstract class ChatMessageHandlerTemplate {

    @Resource
    protected ChatMessageBroadcastUtil chatMessageBroadcastUtil;

    @Resource
    protected ObjectMapper webSocketObjectMapper;

    @Resource
    protected ChatMessageDeleteRedisUtil chatMessageDeleteRedisUtil;

    // 处理在线实时消息
    public abstract void handler(ChatMessage chatMessage,
                                 WebSocketSession session) throws Exception;
    // 发送历史消息
    public abstract void sendChatHistory(Long id, WebSocketSession session);
}
