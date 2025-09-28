package com.xin.graphdomainbackend.infrastructure.websocket.chat.handler;

import com.xin.graphdomainbackend.infrastructure.redis.constant.RedisConstant;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.model.ChatHistoryMessageRequest;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


/**
 * 清理缓存
 */
@Component
public class ChatMessageDeleteRedisUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void deleteRedis(ChatMessage message) {
        long id = 0;
        ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
        Long size = chatHistoryMessageRequest.getSize();
        Long current = chatHistoryMessageRequest.getCurrent();
        String cacheKey = "";
        if (message.getPictureId() != null) {
            id = message.getPictureId();
            cacheKey = RedisConstant.PICTURE_CHAT_HISTORY_PREFIX
                    + id + ":" + current + ":" + size;
        } else if (message.getPrivateChatId() != null) {
            id = message.getPrivateChatId();
            cacheKey = RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX
                    + id + ":" + current + ":" + size;
        } else if (message.getSpaceId() != null) {
            id = message.getSpaceId();
            cacheKey = RedisConstant.SPACE_CHAT_HISTORY_PREFIX
                    + id + ":" + current + ":" + size;
        }

        stringRedisTemplate.delete(cacheKey);
    }
}
