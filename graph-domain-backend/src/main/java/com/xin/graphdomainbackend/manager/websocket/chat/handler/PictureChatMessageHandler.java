package com.xin.graphdomainbackend.manager.websocket.chat.handler;

import com.xin.graphdomainbackend.model.dto.message.chat.ChatHistoryMessageRequest;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.IOException;

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
