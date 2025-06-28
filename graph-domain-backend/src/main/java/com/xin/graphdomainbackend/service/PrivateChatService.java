package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.entity.websocket.PrivateChat;
import com.xin.graphdomainbackend.model.vo.message.PrivateChatVO;

/**
* @author Administrator
* @description 针对表【private_chat(私聊表)】的数据库操作Service
* @createDate 2025-06-12 18:25:02
*/
public interface PrivateChatService extends IService<PrivateChat> {

    /**
     * 创建或更新私聊
     */
    PrivateChatVO sendPrivateMessage(long userId, long targetUserId, String content);

    /**
     * 处理已接收的WebSocket私聊消息，更新会话状态(实时聊天消息)
     *
     * @param chatMessage 聊天消息对象，包含消息内容、发送时间等信息
     * @param privateChatId 私聊会话ID，用于标识唯一的私聊会话
     * @param sender 发送者用户对象，包含用户ID等信息
     */
    void updatePrivateChatWithNewMessage(ChatMessage chatMessage, Long privateChatId, User sender);

    /**
     * 获取查询条件
     * @param privateChatQueryRequest 私人聊天查询请求
     * @param loginUser 登录用户
     */
    QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser);

    /**
     *  分页获取 私人聊天信息
     * @param privateChatQueryRequest 私人聊天查询请求
     * @param loginUser 登录用户
     */
    Page<PrivateChatVO> getPrivateChatByPage(PrivateChatQueryRequest privateChatQueryRequest, User loginUser);
}
