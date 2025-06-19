package com.xin.graphdomainbackend.manager.websocket.chat.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.xin.graphdomainbackend.manager.websocket.chat.ChatWebSocketHandler;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
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

    @Override
    public void onEvent(ChatEvent chatEvent) throws Exception {
        ChatMessage chatMessage = chatEvent.getChatMessage();
        Long targetId = chatEvent.getTargetId();
        WebSocketSession targetSession = chatEvent.getSession();

        try {
            switch (chatEvent.getTargetType()) {
                case 1: //私聊
                    chatMessage.setPrivateChatId(targetId);
                    chatWebSocketHandler.handlePrivateChatMessage(chatMessage, targetSession);
                    break;
                case 2: // 图片聊天室
                    chatMessage.setPictureId(targetId);
                    chatWebSocketHandler.handlePictureChatMessage(chatMessage, targetSession);
                    break;
                case 3: // 空间聊天
                    chatMessage.setSpaceId(targetId);
                    chatWebSocketHandler.handleSpaceChatMessage(chatMessage, targetSession);
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
