package com.xin.graphdomainbackend.infrastructure.websocket.chat;

import com.xin.graphdomainbackend.infrastructure.websocket.chat.enums.ChatMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.ChatMessageHandlerTemplate;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.PictureChatMessageHandler;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.PrivateChatMessageHandler;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.SpaceChatMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息类型（messageType）动态获取对应的消息处理器
 */
@Component
public class ChatMessageHandlerFactory {

    private final Map<String, ChatMessageHandlerTemplate> handlers = new HashMap<>();

    @Autowired
    public ChatMessageHandlerFactory(List<ChatMessageHandlerTemplate> chatMessageHandlerTemplates) {
        for (ChatMessageHandlerTemplate handler : chatMessageHandlerTemplates) {
            if (handler instanceof PictureChatMessageHandler) {
                handlers.put(ChatMessageTypeEnum.PICTURE.getValue(), handler);
            } else if (handler instanceof PrivateChatMessageHandler) {
                handlers.put(ChatMessageTypeEnum.PRIVATE.getValue(), handler);
            } else  if (handler instanceof SpaceChatMessageHandler) {
                handlers.put(ChatMessageTypeEnum.SPACE.getValue(), handler);
            }
        }
    }

    public ChatMessageHandlerTemplate getHandler(String messageType) {
        return handlers.get(messageType);
    }
}
