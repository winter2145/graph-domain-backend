package com.xin.graphdomainbackend.infrastructure.websocket.chat.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.ChatMessageHandlerFactory;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.ChatWebSocketHandler;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.enums.ChatMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.ChatMessageHandlerTemplate;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 聊天事件处理器
 */
@Component
@Slf4j
public class ChatEventWorkHandler implements WorkHandler<ChatEvent> {

    @Resource
    @Lazy
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    private ChatMessageHandlerFactory chatMessageHandlerFactory;

    @Override
    public void onEvent(ChatEvent chatEvent) throws Exception {
        ChatMessage chatMessage = chatEvent.getChatMessage();
        Long targetId = chatEvent.getTargetId();
        WebSocketSession targetSession = chatEvent.getSession();
        ChatMessageHandlerTemplate chatMessageHandlerTemplate;

        try {
            switch (chatEvent.getTargetType()) {
                case 1: //私聊
                    chatMessage.setPrivateChatId(targetId);
                    chatMessageHandlerTemplate = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.PRIVATE.getValue());
                    chatMessageHandlerTemplate.handler(chatMessage, targetSession);
                    break;
                case 2: // 图片聊天室
                    chatMessage.setPictureId(targetId);
                    chatMessageHandlerTemplate = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.PICTURE.getValue());
                    chatMessageHandlerTemplate.handler(chatMessage, targetSession);
                    break;
                case 3: // 空间聊天
                    chatMessage.setSpaceId(targetId);
                    chatMessageHandlerTemplate = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.SPACE.getValue());
                    chatMessageHandlerTemplate.handler(chatMessage, targetSession);
                    break;
                default:
                    log.error("Unknown target type: {}", targetSession);
            }
        } catch (IOException e) {
            log.error("处理聊天消息失败", e);
        } finally {
            chatEvent.clear(); // 清空事件数据
        }
    }
}
