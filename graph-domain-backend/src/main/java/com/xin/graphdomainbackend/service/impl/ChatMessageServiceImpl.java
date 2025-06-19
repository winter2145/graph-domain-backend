package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.mapper.ChatMessageMapper;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatMessageVO;
import com.xin.graphdomainbackend.service.ChatMessageService;
import com.xin.graphdomainbackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【chat_message(聊天信息表)】的数据库操作Service实现
* @createDate 2025-06-12 18:25:02
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Override
    public Page<ChatMessage> getUserChatHistory(long userId, long otherUserId, long current, long size) {

        // 构建分页对象
        Page<ChatMessage> pageRequest = new Page<>(current, size);

        // 构建查询条件：userA 和 userB 的任意发送/接收组合，且类型为私聊（type = 1）
        LambdaQueryWrapper<ChatMessage> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.and(wrapper ->wrapper
                    .or(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, otherUserId))
                    .or(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, otherUserId)))
                .eq(ChatMessage::getType, 1)
                .orderByDesc(ChatMessage::getCreateTime);

        // 查询分页结果
        Page<ChatMessage> chatMessagePage = this.page(pageRequest, lambdaQueryWrapper);
        return chatMessagePage;
    }

    @Override
    public Page<ChatMessage> getPictureChatHistory(long pictureId, long current, long size) {

        // 构建分页对象
        Page<ChatMessage> pageRequest = new Page<>(current, size);

        // 1.存在缓存，查缓存
        String cacheKey = RedisConstant.PICTURE_CHAT_HISTORY_PREFIX
                + pictureId + ":" + current + ":" + size;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            Page<ChatMessage> resultMessage = null;
            try {
                resultMessage = webSocketObjectMapper.readValue(cacheValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize chat history from cache (json -> java)", e);
            }
            return resultMessage;
        }
        // 2.缓存不存在，查数据库
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getPictureId, pictureId)
                .eq(ChatMessage::getType, 2)
                .orderByDesc(ChatMessage::getCreateTime);

        Page<ChatMessage> chatMessagePage = this.page(pageRequest, queryWrapper);

        // 3.更新缓存
        try {
            cacheValue = webSocketObjectMapper.writeValueAsString(chatMessagePage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat history to cache (java -> json)", e);
        }
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300); // 5 - 10 分钟随机过期，防止雪崩
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return chatMessagePage;
    }

    @Override
    public Page<ChatMessage> getSpaceChatHistory(long spaceId, long current, long size) {

        // 构建分页对象
        Page<ChatMessage> pageRequest = new Page<>(current, size);

        // 1.存在缓存，查缓存
        String cacheKey = RedisConstant.SPACE_CHAT_HISTORY_PREFIX
                + spaceId + ":" + current + ":" + size;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            Page<ChatMessage> resultMessage = null;
            try {
                resultMessage = webSocketObjectMapper.readValue(cacheValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize chat history from cache (json -> java)", e);
            }
            return resultMessage;
        }
        // 2.缓存不存在，查数据库
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSpaceId, spaceId)
                .eq(ChatMessage::getType, 3)
                .orderByDesc(ChatMessage::getCreateTime);

        Page<ChatMessage> chatMessagePage = this.page(pageRequest, queryWrapper);

        // 3.更新缓存
        try {
            cacheValue = webSocketObjectMapper.writeValueAsString(chatMessagePage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat history to cache (java -> json)", e);
        }
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300); // 5 - 10 分钟随机过期，防止雪崩
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return chatMessagePage;
    }

    @Override
    public Page<ChatMessage> getPrivateChatHistory(long privateChatId, long current, long size) {
        // 构建分页对象
        Page<ChatMessage> pageRequest = new Page<>(current, size);

        // 1.存在缓存，查缓存
        String cacheKey = RedisConstant.PRIVATE_CHAT_HISTORY_PREFIX
                + privateChatId + ":" + current + ":" + size;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            Page<ChatMessage> resultMessage = null;
            try {
                resultMessage = webSocketObjectMapper.readValue(cacheValue, new TypeReference<Page<ChatMessage>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize chat history from cache (json -> java)", e);
            }
            return resultMessage;
        }
        // 2.缓存不存在，查数据库
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getPrivateChatId, privateChatId)
                .eq(ChatMessage::getType, 1)
                .orderByDesc(ChatMessage::getCreateTime);

        Page<ChatMessage> chatMessagePage = this.page(pageRequest, queryWrapper);

        // 3.更新缓存
        try {
            cacheValue = webSocketObjectMapper.writeValueAsString(chatMessagePage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat history to cache (java -> json)", e);
        }
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300); // 5 - 10 分钟随机过期，防止雪崩
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return chatMessagePage;
    }

    @Override
    public ChatHistoryPageResponse getUserChatHistoryVO(long userId, long otherUserId, long current, long size) {
        Page<ChatMessage> userChatHistoryPage = this.getUserChatHistory(userId, otherUserId, current, size);

        List<ChatMessageVO> chatMessageVOList = userChatHistoryPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ChatHistoryPageResponse response = new ChatHistoryPageResponse();
        response.setRecords(chatMessageVOList);
        response.setCurrent(userChatHistoryPage.getCurrent());
        response.setSize(userChatHistoryPage.getSize());
        response.setTotal(userChatHistoryPage.getTotal());

        return response;
    }

    @Override
    public ChatHistoryPageResponse getPictureChatHistoryVO(long pictureId, long current, long size) {
        Page<ChatMessage> pictureChatHistoryPage = this.getPictureChatHistory(pictureId, current, size);

        List<ChatMessageVO> chatMessageVOList = pictureChatHistoryPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ChatHistoryPageResponse response = new ChatHistoryPageResponse();
        response.setRecords(chatMessageVOList);
        response.setCurrent(pictureChatHistoryPage.getCurrent());
        response.setSize(pictureChatHistoryPage.getSize());
        response.setTotal(pictureChatHistoryPage.getTotal());

        return response;
    }

    @Override
    public ChatHistoryPageResponse getSpaceChatHistoryVO(long spaceId, long current, long size) {
        Page<ChatMessage> chatMessagePage = this.getSpaceChatHistory(spaceId, current, size);

        List<ChatMessageVO> chatMessageVOList = chatMessagePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ChatHistoryPageResponse response = new ChatHistoryPageResponse();
        response.setRecords(chatMessageVOList);
        response.setCurrent(chatMessagePage.getCurrent());
        response.setSize(chatMessagePage.getSize());
        response.setTotal(chatMessagePage.getTotal());

        return response;
    }

    @Override
    public ChatHistoryPageResponse getPrivateChatHistoryVO(long privateChatId, long current, long size) {
        Page<ChatMessage> chatMessagePage = this.getSpaceChatHistory(privateChatId, current, size);

        List<ChatMessageVO> chatMessageVOList = chatMessagePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ChatHistoryPageResponse response = new ChatHistoryPageResponse();
        response.setRecords(chatMessageVOList);
        response.setCurrent(chatMessagePage.getCurrent());
        response.setSize(chatMessagePage.getSize());
        response.setTotal(chatMessagePage.getTotal());

        return response;
    }

    /**
     * ChatMessage -> ChatMessageVO
     * @param message
     * @return
     */
    public ChatMessageVO convertToVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);

        Long senderId = message.getSenderId();
        User user = userService.getById(senderId);
        if (user != null) {
            vo.setSenderName(user.getUserName());
            vo.setSenderAvatar(user.getUserAvatar());
        }
        return vo;
    }

}




