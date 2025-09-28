package com.xin.graphdomainbackend.privatechat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.handler.ChatMessageSessionService;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.service.ChatMessageService;
import com.xin.graphdomainbackend.privatechat.api.dto.request.PrivateChatQueryRequest;
import com.xin.graphdomainbackend.privatechat.api.dto.vo.PrivateChatVO;
import com.xin.graphdomainbackend.privatechat.dao.entity.PrivateChat;
import com.xin.graphdomainbackend.privatechat.dao.mapper.PrivateChatMapper;
import com.xin.graphdomainbackend.privatechat.service.PrivateChatService;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import com.xin.graphdomainbackend.userfollows.api.dto.request.UserFollowsIsFollowsRequest;
import com.xin.graphdomainbackend.userfollows.service.UserFollowsService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

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

    @Resource
    private ChatMessageSessionService sessionService;


    @Override
    public PrivateChatVO sendPrivateMessage(long senderId, long receiverId, String content) {
        // 1. 权限判断
        checkCanSendMessage(senderId, receiverId);

        // 2. 获取或创建聊天记录
        PrivateChat privateChat = getOrCreatePrivateChat(senderId, receiverId, content);

        // 3. 创建消息记录
        saveChatMessage(senderId, receiverId, content);

        return getPrivateChatVO(privateChat);
    }

    @Override
    public void updatePrivateChatWithNewMessage(ChatMessage chatMessage, Long privateChatId, User sender) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if(privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 检查对话是否被任一方删除
        if (privateChat.isUserDeleted() || privateChat.isTargetUserDeleted()) {
            // 如果对话被任一方删除，重置未读计数
            if (privateChat.getUserId().equals(sender.getId())) {
                privateChat.setTargetUserUnreadCount(1);  // 对方未读重置为1
                privateChat.setTargetUserDeleted(false);  // 恢复对话
            } else {
                privateChat.setUserUnreadCount(1);        // 对方未读重置为1
                privateChat.setUserDeleted(false);        // 恢复对话
            }
        } else {
            if (privateChat.getUserId().equals(sender.getId())) {

                // 获取私聊聊天室在线人数
                Set<WebSocketSession> sessions = sessionService.getPrivateChatSessions(privateChatId);
                boolean bothOnline = sessions != null && sessions.size() == 2;

                // 只有在 1 方不在线时,才增加未读消息数
                if (!bothOnline) {
                    privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);
                }

            } else if (privateChat.getTargetUserId().equals(sender.getId())) {

                // 获取私聊聊天室在线人数
                Set<WebSocketSession> sessions = sessionService.getPrivateChatSessions(privateChatId);
                boolean bothOnline = sessions != null && sessions.size() == 2;

                // 只有在 1 方不在线时,才增加未读消息数
                if (!bothOnline) {
                    privateChat.setUserUnreadCount(privateChat.getUserUnreadCount() + 1);
                }
            } else {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
            }
        }
        // 设置消息接收者
        Long receiverId = privateChat.getUserId().equals(sender.getId())
                ? privateChat.getTargetUserId()
                : privateChat.getUserId();
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
        queryWrapper.nested(wrap ->
                wrap.eq("userId", userId)
                        .ne("targetUserId", userId)
                        .eq("userDeleted", 0) // 只查当前用户未删除的
        ).or(wrap ->
                wrap.eq("targetUserId", userId)
                        .ne("userId", userId)
                        .eq("targetUserDeleted", 0) // 只查目标用户未删除的
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
    @Transactional(readOnly = true)
    public Page<PrivateChatVO> getPrivateChatByPage(PrivateChatQueryRequest privateChatQueryRequest, User loginUser) {
        ThrowUtils.throwIf(privateChatQueryRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        long current = privateChatQueryRequest.getCurrent();
        long size = privateChatQueryRequest.getPageSize();

        // 查询相关的聊天
        Page<PrivateChat> page = this.page(new Page<>(current, size),
                this.getQueryWrapper(privateChatQueryRequest, loginUser));

        // 3. 处理空结果情况
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return new Page<>(privateChatQueryRequest.getCurrent(),
                    privateChatQueryRequest.getPageSize());
        }

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

        // 3.判断当前用户是 userId 还是 targetUserId
        boolean isUserSide = privateChat.getUserId().equals(loginUser.getId());

        // 4.逻辑删除
        if (isUserSide) {
            // 如果是 userId 删除，则标记 userId 的删除状态
            privateChat.setUserDeleted(true);
        } else {
            // 如果是 targetUserId 删除，则标记 targetUserId 的删除状态
            privateChat.setTargetUserDeleted(true);
        }

        // 5.更新数据库（不删除记录，仅标记删除状态）
        boolean success = this.updateById(privateChat);

        // 6.如果双方都删除了，才真正删除记录
        if (privateChat.isUserDeleted() && privateChat.isTargetUserDeleted()) {
            this.removeById(privateChatId);
            LambdaQueryWrapper<ChatMessage> chatMessageQuery = new LambdaQueryWrapper<>();
            chatMessageQuery.eq(ChatMessage::getPrivateChatId, privateChatId);
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

            // 查找对应的用户名
            List<User> list = userService.lambdaQuery()
                    .eq(User::getId, userId)
                    .or()
                    .eq(User::getId, targetUserId)
                    .list();
            Map<Long, String> userNameMap = list.stream()
                    .filter(user -> user.getId() != null && user.getUserName() != null)
                    .collect(Collectors.toMap(
                            User::getId,      // 键的映射函数
                            User::getUserName // 值的映射函数
                    ));

            chat.setTargetUserChatName(userNameMap.get(userId));
            chat.setUserChatName(userNameMap.get(targetUserId));

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
        // 只保存有发送内容的那条数据
        if (content != null) {
            ChatMessage message = new ChatMessage();
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setContent(content);
            message.setType(1); // 私聊
            message.setStatus(0); // 未读
            chatMessageService.save(message);
        }
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




