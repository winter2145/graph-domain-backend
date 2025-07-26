package com.xin.graphdomainbackend.manager.websocket.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Slf4j
public abstract class ChatMessageHandlerTemplate {

    @Resource
    protected ChatMessageBroadcastUtil chatMessageBroadcastUtil;

    @Resource
    protected ObjectMapper webSocketObjectMapper;

    @Resource
    protected ChatMessageDeleteRedisUtil chatMessageDeleteRedisUtil;

    public abstract void handler(ChatMessage chatMessage,
                                 WebSocketSession session) throws Exception;

    public abstract void sendChatHistory(Long id, WebSocketSession session);
}
