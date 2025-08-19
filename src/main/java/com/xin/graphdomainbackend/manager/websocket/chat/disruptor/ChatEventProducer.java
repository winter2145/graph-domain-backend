package com.xin.graphdomainbackend.manager.websocket.chat.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 聊天事件生产者
 */
@Component
@Slf4j
public class ChatEventProducer {

    @Resource
    private Disruptor<ChatEvent> chatEventDisruptor;

    public void publishEvent(ChatMessage chatMessage, WebSocketSession session,
                             User user, Long targetId, Integer targetType) {
        // 获取 Disruptor 的环形缓冲区 RingBuffer
        RingBuffer<ChatEvent> ringBuffer = chatEventDisruptor.getRingBuffer();

        // next() 获取当前可写入的槽位
        long sequence = ringBuffer.next();

        ChatEvent event = ringBuffer.get(sequence);
        event.setChatMessage(chatMessage);
        event.setSession(session);
        event.setTargetId(targetId);
        event.setUser(user);
        event.setTargetType(targetType);

        // 发布事件,通知消费者消费
        ringBuffer.publish(sequence);
    }

    @PreDestroy
    //当 Spring 应用关闭时,关闭 Disruptor
    public void destroy() {
        chatEventDisruptor.shutdown();
    }

}