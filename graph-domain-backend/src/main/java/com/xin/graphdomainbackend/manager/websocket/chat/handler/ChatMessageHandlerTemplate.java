package com.xin.graphdomainbackend.manager.websocket.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

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
