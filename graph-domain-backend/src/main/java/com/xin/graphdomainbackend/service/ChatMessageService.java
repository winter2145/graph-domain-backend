package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;

/**
* @author Administrator
* @description 针对表【chat_message(聊天信息表)】的数据库操作Service
* @createDate 2025-06-12 18:25:02
*/
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 获取指定用户的聊天记录
     * @param userId 用户
     * @param otherUserId 用户
     * @param current 当前页号
     * @param size 每页大小
     */
    Page<ChatMessage> getUserChatHistory(long userId, long otherUserId, long current, long size);

    /**
     * 获取指定图片的聊天记录
     * @param pictureId 图片id
     * @param current 当前页号
     * @param size 每页大小
     */
    Page<ChatMessage> getPictureChatHistory(long pictureId, long current, long size);

    /**
     * 获取指定空间的聊天记录
     * @param spaceId 空间id
     * @param current 当前页号
     * @param size 每页大小
     */
    Page<ChatMessage> getSpaceChatHistory(long spaceId, long current, long size);

    /**
     * 获取私聊历史消息
     * @param privateChatId 私聊ID
     * @param current 当前页
     * @param size 每页大小
     * @return 消息分页数据
     */
    Page<ChatMessage> getPrivateChatHistory(long privateChatId, long current, long size);

    /**
     * 获取当前用户与另一个用户的私聊历史消息（分页 + VO 封装）
     *
     * @param userId 当前用户 ID
     * @param otherUserId 对方用户 ID
     * @param current 当前页码
     * @param size 每页数量
     */
    ChatHistoryPageResponse getUserChatHistoryVO(long userId, long otherUserId, long current, long size);

    /**
     * 获取指定图片聊天室的聊天历史记录（分页 + VO 封装）
     *
     * @param pictureId 图片聊天室对应的图片 ID
     * @param current 当前页码
     * @param size 每页数量
     */
    ChatHistoryPageResponse getPictureChatHistoryVO(long pictureId, long current, long size);

    /**
     * 获取指定空间内的聊天室聊天记录（分页 + VO 封装）
     *
     * @param spaceId 空间 ID
     * @param current 当前页码
     * @param size 每页数量
     */
    ChatHistoryPageResponse getSpaceChatHistoryVO(long spaceId, long current, long size);

    /**
     * 获取指定私聊会话 ID 的聊天记录（分页 + VO 封装）
     *
     * @param privateChatId 私聊会话 ID
     * @param current 当前页码
     * @param size 每页数量
     */
    ChatHistoryPageResponse getPrivateChatHistoryVO(long privateChatId, long current, long size);

}
