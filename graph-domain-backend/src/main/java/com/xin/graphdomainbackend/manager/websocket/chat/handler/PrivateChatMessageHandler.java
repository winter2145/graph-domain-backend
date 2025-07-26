package com.xin.graphdomainbackend.manager.websocket.chat.handler;

import com.xin.graphdomainbackend.constant.WebSocketConstant;
import com.xin.graphdomainbackend.model.dto.message.chat.ChatHistoryMessageRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.service.ChatMessageService;
import com.xin.graphdomainbackend.service.PrivateChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Component
@Slf4j
public class PrivateChatMessageHandler extends ChatMessageHandlerTemplate{

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private PrivateChatService privateChatService;

    @Override
    public void handler(ChatMessage chatMessage, WebSocketSession session) throws Exception {

        // 获取 user对象
        User user = (User) session.getAttributes().get(WebSocketConstant.USER);
        // 保存消息
        chatMessageService.save(chatMessage);

        // 登录消息后清除缓存
        chatMessageDeleteRedisUtil.deleteRedis(chatMessage);

        privateChatService.updatePrivateChatWithNewMessage(chatMessage, chatMessage.getPrivateChatId(), user);

        // 发送消息
        chatMessageBroadcastUtil.sendToPrivateRoom(chatMessage);
    }

    @Override
    public void sendChatHistory(Long privateChatId, WebSocketSession session) {
        try {
            ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
            Long size = chatHistoryMessageRequest.getSize();
            Long current = chatHistoryMessageRequest.getCurrent();
            // 获取私人历史消息VO
            ChatHistoryPageResponse history = chatMessageService.getPrivateChatHistoryVO(privateChatId, current, size);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(history)));
        } catch (Exception e) {
            log.error("发送历史消息失败", e);
        }
    }
}
