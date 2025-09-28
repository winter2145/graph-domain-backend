package com.xin.graphdomainbackend.infrastructure.websocket.chat.handler;

import com.xin.graphdomainbackend.infrastructure.websocket.chat.model.ChatHistoryMessageRequest;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.model.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.service.ChatMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 图片聊天消息处理
 */
@Component
@Slf4j
public class PictureChatMessageHandler extends ChatMessageHandlerTemplate{
    @Resource
    private ChatMessageService chatMessageService;


    @Override
    public void handler(ChatMessage chatMessage, WebSocketSession session) throws IOException{
            // 保存消息
            chatMessageService.save(chatMessage);

            chatMessageDeleteRedisUtil.deleteRedis(chatMessage);

            // 发送消息
            chatMessageBroadcastUtil.sendToPictureRoom(chatMessage);
    }

    @Override
    public void sendChatHistory(Long pictureId, WebSocketSession session) {
        try {
            ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
            Long size = chatHistoryMessageRequest.getSize();
            Long current = chatHistoryMessageRequest.getCurrent();
            // 获取图片聊天室历史消息VO
            ChatHistoryPageResponse history = chatMessageService.getPictureChatHistoryVO(pictureId, current, size);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(history)));
        } catch (Exception e) {
            log.error("发送历史消息失败", e);
        }
    }
}
