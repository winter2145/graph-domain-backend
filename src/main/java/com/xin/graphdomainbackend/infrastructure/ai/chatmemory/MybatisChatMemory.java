package com.xin.graphdomainbackend.infrastructure.ai.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatMessageMapper;
import com.xin.graphdomainbackend.infrastructure.ai.constant.AiConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MybatisChatMemory implements ChatMemory {

    @Resource
    private AiChatMessageMapper messageMapper;

    /**
     * 将消息列表持久化到数据库，messages 按传入顺序写入（通常是按时间顺序）。
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // 解析 conversationId 获取 sessionId 和 roundId
        String[] parts = conversationId.split("_draw_");
        if (parts.length != 2) {
            return;
        }

        Long sessionId = Long.valueOf(parts[0]);
        Long roundId = Long.valueOf(parts[1]);

        for (Message msg : messages) {
            AiChatMessage entity = new AiChatMessage();
            entity.setSessionId(sessionId);
            entity.setRoundId(roundId);
            entity.setRole(msg.getMessageType().getValue());
            entity.setContent(msg.getText());
            entity.setCreateTime(LocalDateTime.now());
            messageMapper.insert(entity);
        }
    }

    /**
     * 获取会话中最近的 lastN 条消息，并按时间升序返回（从早到晚）。
     * 如果 lastN <= 0 返回空列表。
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        // 解析 conversationId 获取 sessionId 和 roundId
        String[] parts = conversationId.split("_draw_");
        if (parts.length != 2) {
            return List.of();
        }

        Long sessionId = Long.valueOf(parts[0]);
        Long roundId = Long.valueOf(parts[1]);

        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getRoundId, roundId)
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .last("LIMIT " + lastN)
        );
        log.info("get messages: {}", messages);

        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Collections.reverse(messages);

        return messages.stream()
                .map(m -> {
                    return switch (m.getRole()) {
                        case AiConstant.USER_ROLE -> new UserMessage(m.getContent());
                        case AiConstant.ASSISTANT_ROLE -> new AssistantMessage(m.getContent());
                        case AiConstant.SYSTEM_ROLE -> new SystemMessage(m.getContent());
                        default -> throw new IllegalArgumentException("Unknown role: " + m.getRole());
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * 清空会话历史
     */
    @Override
    public void clear(String conversationId) {
        String[] parts = conversationId.split("_draw_");
        if (parts.length != 2) {
            return;
        }

        Long sessionId = Long.valueOf(parts[0]);
        Long roundId = Long.valueOf(parts[1]);

        messageMapper.delete(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getRoundId, roundId)
        );
    }
}