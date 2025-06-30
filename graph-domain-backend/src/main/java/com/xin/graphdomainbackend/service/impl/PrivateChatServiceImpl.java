package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.websocket.chat.ChatWebSocketHandler;
import com.xin.graphdomainbackend.mapper.PrivateChatMapper;
import com.xin.graphdomainbackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.entity.websocket.PrivateChat;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.model.vo.message.PrivateChatVO;
import com.xin.graphdomainbackend.service.ChatMessageService;
import com.xin.graphdomainbackend.service.PrivateChatService;
import com.xin.graphdomainbackend.service.UserFollowsService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【private_chat(私聊表)】的数据库操作Service实现
* @createDate 2025-06-12 18:25:02
*/
@Service
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatMapper, PrivateChat>
    implements PrivateChatService {

    @Resource
    private UserFollowsService userFollowsService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserService userService;


    @Override
    public PrivateChatVO sendPrivateMessage(long senderId, long receiverId, String content) {
        // 1. 权限判断
        checkCanSendMessage(senderId, receiverId);

        // 2. 获取或创建聊天记录
        PrivateChat privateChat = getOrCreatePrivateChat(senderId, receiverId, content);

        // 3. 创建消息记录
        saveChatMessage(senderId, receiverId, content);

        // 4. 更新未读消息数
        updateUnreadCount(privateChat, senderId);

        return getPrivateChatVO(privateChat);
    }

    @Override
    public void updatePrivateChatWithNewMessage(ChatMessage chatMessage, Long privateChatId, User sender) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if(privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前私聊记录，确定接收者id
        Long receiverId;
        if (privateChat.getUserId().equals(sender.getId())) {
            receiverId = privateChat.getTargetUserId();

            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketHandler.getPrivateChatSessions(privateChatId);
            boolean bothOnline = sessions != null && sessions.size() == 2;
            // 只有在双方不都在线时才增加未读消息数
            if (!bothOnline) {
                privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);
            }
        } else if (privateChat.getTargetUserId().equals(sender.getId())) {
            receiverId = privateChat.getUserId();
            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketHandler.getPrivateChatSessions(privateChatId);
            boolean bothOnline = sessions != null && sessions.size() == 2;
            // 只有在双方不都在线时才增加未读消息数
            if (!bothOnline) {
                privateChat.setUserUnreadCount(privateChat.getUserUnreadCount() + 1);
            }
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }
        chatMessage.setReceiverId(receiverId);
        chatMessage.setSenderId(sender.getId());

        // 更新私聊记录的最后一句内容
        privateChat.setLastMessage(chatMessage.getContent());
        privateChat.setLastMessageTime(new Date());

        // 保存更新
        this.updateById(privateChat);
    }

    @Override
    public QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser) {
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        if (privateChatQueryRequest == null) {
            return queryWrapper;
        }

        // 使用 final 修饰需要在 lambda 中使用的变量
        final Long userId = loginUser.getId();
        final Long targetUserId = privateChatQueryRequest.getTargetUserId();
        final Integer chatType = privateChatQueryRequest.getChatType();
        final String searchText = privateChatQueryRequest.getSearchText();

        // 查询与当前用户相关的聊天记录，并且排除自己和自己的对话
        queryWrapper.and(wrap ->
                wrap.eq("userId", userId).ne("targetUserId", userId)
                        .or()
                        .eq("targetUserId", userId).ne("userId", userId)
        );
        // 如果指定了目标用户，则只查询与该用户的对话
        if (targetUserId != null && targetUserId > 0) {
            queryWrapper.and(wrap ->
                    wrap.eq("targetUserId", targetUserId).eq("userId", userId)
                            .or()
                            .eq("userId", targetUserId).eq("targetUserId", userId)
            );
        }
        // 如果指定了聊天类型（私聊/好友），则按类型筛选
        if (chatType != null) {
            queryWrapper.eq("chatType", chatType);
        }

        // 搜索最后一条消息
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("lastMessage", searchText);
        }
        // 按最后消息时间倒序
        queryWrapper.orderByDesc("lastMessageTime");

        return queryWrapper;
    }

    @Override
    public Page<PrivateChatVO> getPrivateChatByPage(PrivateChatQueryRequest privateChatQueryRequest, User loginUser) {
        ThrowUtils.throwIf(privateChatQueryRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        long current = privateChatQueryRequest.getCurrent();
        long size = privateChatQueryRequest.getPageSize();

        // 查询相关的聊天
        Page<PrivateChat> page = this.page(new Page<>(current, size),
                this.getQueryWrapper(privateChatQueryRequest, loginUser));

        // 提前收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        page.getRecords().forEach(privateChat -> {
            userIds.add(privateChat.getUserId()); // 发送者id
            if (privateChat.getTargetUserId() != null) {
                userIds.add(privateChat.getTargetUserId()); // 接收者id
            }
        });

        // 批量查询用户信息并转换为Map
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        userService::getUserVO
                ));

        Long loginUserId = loginUser.getId();


        // 转换记录
        List<PrivateChatVO> voList = page.getRecords().stream()
                .map(privateChat -> {
                    PrivateChatVO vo = new PrivateChatVO();
                    BeanUtils.copyProperties(privateChat, vo);

                    // 设置发送者标识
                    boolean isSender = loginUserId.equals(privateChat.getUserId());
                    vo.setIsSender(isSender);

                    // 第一次消息发起者用户的VO
                    UserVO currentTargetUerVO = userVOMap.get(privateChat.getUserId());

                    // 根据是否发送者设置 目标用户信息
                    if (isSender) {
                        // 发送者：targetUser是 接收者
                        vo.setTargetUser(userVOMap.get(privateChat.getTargetUserId()));
                    } else {
                        // 接收者：targetUser是 发送者
                        vo.setTargetUser(currentTargetUerVO);
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        // 构建返回结果
        Page<PrivateChatVO> result = new Page<>(current, size);
        result.setRecords(voList);
        result.setTotal(page.getTotal());

        return result;
    }

    @Override
    public void clearUnreadCount(long userId, long targetUserId, boolean isSender) {
        // 如果当前用户不是发送者，需要交换userId和targetUserId
        if (isSender) {
            long temp = userId;
            userId = targetUserId;
            targetUserId = temp;
        }

        this.update()
                .set("userUnreadCount", 0)  // 清除发送者的未读消息数
                .eq("userId", userId)
                .eq("targetUserId", targetUserId)
                .update();

        // 同时处理可能存在的反向记录
        this.update()
                .set("targetUserUnreadCount", 0)  // 清除接收者的未读消息数
                .eq("userId", targetUserId)
                .eq("targetUserId", userId)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePrivateChat(Long privateChatId, User loginUser) {

        // 1.获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        ThrowUtils.throwIf(privateChat == null, ErrorCode.NOT_FOUND_ERROR);

        // 2.校验权限，只有私聊参与者才能删除
        if (!privateChat.getUserId().equals(loginUser.getId())
                && !privateChat.getTargetUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        //3.删除私人聊天记录
        boolean success = this.removeById(privateChatId);
        if (success) {
            // 4.删除相关的聊天消息
            LambdaQueryWrapper<ChatMessage> chatMessageQuery = new LambdaQueryWrapper<>();
            chatMessageQuery.eq(ChatMessage::getPrivateChatId, privateChatId)
                    .eq(ChatMessage::getType, 1);
            chatMessageService.remove(chatMessageQuery);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChatName(Long privateChatId, String chatName, User loginUser) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前用户身份更新对应的聊天名称
        if (privateChat.getUserId().equals(loginUser.getId())) {
            privateChat.setUserChatName(chatName);
        } else if (privateChat.getTargetUserId().equals(loginUser.getId())) {
            privateChat.setTargetUserChatName(chatName);
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        this.updateById(privateChat);
    }

    @Override
    public void updateChatType(Long userId, Long targetUserId, Boolean isFriend) {
        int chatType = isFriend ? 1 : 0;

        // 同时更新两个方向的记录
        this.update() // 开始构建更新操作
                .set("chatType", chatType)
                .and(wrap -> wrap
                        .eq("userId", userId).eq("targetUserId", targetUserId)
                        .or()
                        .eq("userId", targetUserId).eq("targetUserId", userId)
                )
                .update(); //执行 SQL 语句
    }

    private void checkCanSendMessage(long senderId, long receiverId) {

        UserFollowsIsFollowsRequest isFollowsRequest = new UserFollowsIsFollowsRequest(senderId, receiverId);
        boolean isFollowed = userFollowsService.findIsFollow(isFollowsRequest);
        if (!isFollowed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你未关注对方，不能发送消息");
        }

        boolean isMutual = userFollowsService.isMutualRelations(senderId, receiverId);
        if (!isMutual) {
            boolean hasSent = chatMessageService.hasSentMessage(senderId, receiverId);
            if (hasSent) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "单向关注只能发送一条消息");
            }
        }
    }

    private PrivateChat getOrCreatePrivateChat(long userA, long userB, String content) {
        long userId = userA;
        long targetUserId = userB;

        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetUserId", targetUserId)
                .or()
                .eq("userId", targetUserId)
                .eq("targetUserId", userId);

        PrivateChat chat = this.getOne(queryWrapper);

        boolean isMutual = userFollowsService.isMutualRelations(userA, userB);
        int chatType = isMutual ? 1 : 0;

        if (chat == null) {
            chat = new PrivateChat();
            chat.setUserId(userId);
            chat.setTargetUserId(targetUserId);
            chat.setUserUnreadCount(0);
            chat.setTargetUserUnreadCount(0);
            chat.setChatType(chatType);
        } else {
            chat.setChatType(chatType); // 更新聊天类型
        }

        chat.setLastMessage(content);
        chat.setLastMessageTime(new Date());

        // 保存私人聊天消息
        this.saveOrUpdate(chat);
        return chat;
    }

    private void saveChatMessage(long senderId, long receiverId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType(1); // 私聊
        message.setStatus(0); // 未读
        chatMessageService.save(message);
    }

    private void updateUnreadCount(PrivateChat chat, long senderId) {
        if (chat.getUserId() == senderId) {
            chat.setTargetUserUnreadCount(chat.getTargetUserUnreadCount() + 1);
        } else {
            chat.setUserUnreadCount(chat.getUserUnreadCount() + 1);
        }
        this.updateById(chat);
    }

    private PrivateChatVO getPrivateChatVO(PrivateChat privateChat) {
        PrivateChatVO privateChatVO = new PrivateChatVO();
        if (privateChat == null) {
            return privateChatVO;
        }
        BeanUtils.copyProperties(privateChat, privateChatVO);

        // 填充目标用户信息
        if (privateChat.getTargetUserId() != null) {
            User targetUser = userService.getById(privateChat.getTargetUserId());
            UserVO userVO = userService.getUserVO(targetUser);
            privateChatVO.setTargetUser(userVO);
        }
        return privateChatVO;
    }
}




