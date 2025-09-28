package com.xin.graphdomainbackend.infrastructure.websocket.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import com.xin.graphdomainbackend.infrastructure.websocket.chat.model.ChatMessageVO;
import com.xin.graphdomainbackend.infrastructure.websocket.constant.WebSocketConstant;
import com.xin.graphdomainbackend.privatechat.dao.entity.PrivateChat;
import com.xin.graphdomainbackend.privatechat.service.PrivateChatService;
import com.xin.graphdomainbackend.spaceuser.service.SpaceUserService;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天广播工具类
 */
@Component
@Slf4j
public class ChatMessageBroadcastUtil {

    @Resource
    private ChatMessageSessionService chatMessageSessionService;

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 广播在线用户信息
     * 为私聊添加特殊处理，使用全局在线状态
     *
     *@param pictureId 图片id
     *@param spaceId 空间id
     *@param privateChatId 私人聊天id
     */
    public void broadcastOnlineUsers(Long pictureId, Long spaceId, Long privateChatId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", WebSocketConstant.ONLINE_USERS);

            Set<WebSocketSession> targetSessions = null;
            if (pictureId != null) {
                targetSessions = chatMessageSessionService.getPictureSessions(pictureId);
                // 获取该图片聊天室的在线用户
                Set<UserVO> onlineUsers = getOnlineUsers(targetSessions);
                response.put(WebSocketConstant.ONLINE_COUNT, onlineUsers.size());
                response.put(WebSocketConstant.ONLINE_USERS, onlineUsers);
                response.put(WebSocketConstant.PICTURE_ID, pictureId);
            } else if (privateChatId != null) {
                targetSessions = chatMessageSessionService.getPrivateChatSessions(privateChatId);

                // 获取私聊信息，找到参与者
                PrivateChat privateChat = privateChatService.getById(privateChatId);
                if (privateChat != null) {
                    User user1 = userService.getById(privateChat.getUserId());
                    User user2 = userService.getById(privateChat.getTargetUserId());

                    Set<UserVO> completeUserList = new HashSet<>();
                    if (user1 != null) {
                        UserVO vo1 = userService.getUserVO(user1);
                        vo1.setOnline(isUserOnline(user1.getId())); // 只查Redis
                        completeUserList.add(vo1);
                    }
                    if (user2 != null) {
                        UserVO vo2 = userService.getUserVO(user2);
                        vo2.setOnline(isUserOnline(user2.getId())); // 只查Redis
                        completeUserList.add(vo2);
                    }

                    response.put(WebSocketConstant.ONLINE_COUNT, completeUserList.stream().filter(UserVO::getOnline).count());
                    response.put(WebSocketConstant.ONLINE_USERS, completeUserList);
                    response.put(WebSocketConstant.PRIVATE_CHAT_ID, privateChatId);
                }
            } else if (spaceId != null) {
                targetSessions = chatMessageSessionService.getSpaceSessions(spaceId);
                // 获取该空间内的在线用户
                Set<UserVO> onlineUsers = getOnlineUsers(targetSessions);
                // 获取空间所有成员
                List<UserVO> allMembers = spaceUserService.getAllSpaceMembers(spaceId);
                // 计算离线用户
                Set<UserVO> offlineUsers = new HashSet<>();
                for (UserVO member : allMembers) {
                    boolean isOnline = onlineUsers.stream()
                            .anyMatch(onlineUser -> onlineUser.getId().equals(member.getId()));
                    if (!isOnline) {
                        offlineUsers.add(member);
                    }
                }
                response.put(WebSocketConstant.ONLINE_COUNT, onlineUsers.size());
                response.put(WebSocketConstant.ONLINE_USERS, onlineUsers);
                response.put(WebSocketConstant.SPACE_ID, spaceId);
                response.put(WebSocketConstant.OFFLINE_COUNT, offlineUsers.size());
                response.put(WebSocketConstant.OFFLINE_USERS, offlineUsers);
                response.put(WebSocketConstant.TOTAL_COUNT, allMembers.size());
            } else {
                return;
            }

            if (targetSessions != null) {
                String messageStr = webSocketObjectMapper.writeValueAsString(response);
                for (WebSocketSession session : targetSessions) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(messageStr));
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播在线用户信息失败", e);
        }
    }

    /**
     * 检查用户列表中是否包含指定ID的用户
     */
    private boolean containsUser(Set<UserVO> users, Long userId) {
        return users.stream().anyMatch(u -> u.getId().equals(userId));
    }

    /**
     * 根据session列表 获取 用户列表
     * @param targetSessions Set session 集合
     */
    private Set<UserVO> getOnlineUsers(Set<WebSocketSession> targetSessions) {
        if (targetSessions != null) {
            return targetSessions.stream()
                    .map(s -> (User) s.getAttributes().get(WebSocketConstant.USER))
                    .filter(Objects::nonNull)
                    .map(user -> {
                        UserVO vo = userService.getUserVO(user);
                        // vo.setOnline(isUserOnline(user.getId())); // 设置全局在线状态
                        return vo;
                    })
                    .collect(Collectors.toSet());
        }
        return Collections.EMPTY_SET;
    }

    /**
     * 广播消息到指定会话集合
     *
     * @param sessions 指定会话
     * @param messageVO 聊天消息
     */
    private void broadcastChatMessage(Set<WebSocketSession> sessions, ChatMessageVO messageVO) throws IOException {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String messageStr = webSocketObjectMapper.writeValueAsString(messageVO);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(messageStr));
            }
        }
    }

    /**
     * 发送图片聊天消息
     */
    public void sendToPictureRoom(ChatMessage message) throws IOException {
        if (message.getPictureId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> pictureSessions = chatMessageSessionService.getPictureSessions(message.getPictureId());
        broadcastChatMessage(pictureSessions, convertToVO(message));
    }

    /**
     * 发送私人聊天消息
     */
    public void sendToPrivateRoom(ChatMessage message) throws IOException {
        if (message.getPrivateChatId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> privateChatSessions = chatMessageSessionService.getPrivateChatSessions(message.getPrivateChatId());
        broadcastChatMessage(privateChatSessions, convertToVO(message));
    }

    /**
     * 发送空间聊天消息
     */
    public void sendToSpaceRoom(ChatMessage message) throws IOException {
        if (message.getSpaceId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> spaceSessions = chatMessageSessionService.getSpaceSessions(message.getSpaceId() );
        broadcastChatMessage(spaceSessions, convertToVO(message));
    }

    /**
     * chatMessage -> ChatMessageVO
     */
    private ChatMessageVO convertToVO(ChatMessage chatMessage) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(chatMessage.getId());
        vo.setContent(chatMessage.getContent());
        vo.setSenderId(chatMessage.getSenderId());
        vo.setCreateTime(chatMessage.getCreateTime());
        vo.setPictureId(chatMessage.getPictureId());
        vo.setSpaceId(chatMessage.getSpaceId());
        vo.setPrivateChatId(chatMessage.getPrivateChatId());
        vo.setReplyId(chatMessage.getReplyId());
        vo.setRootId(chatMessage.getRootId());
        vo.setType(chatMessage.getType());

        // 发送者信息
        User sender = userService.getById(chatMessage.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getUserName());
            vo.setSenderAvatar(sender.getUserAvatar());
        }
        return vo;
    }

    /**
     * 错误消息信息
     */
    public void sendErrorMessage(WebSocketSession session, String message)
            throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("message", message);
        session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(errorResponse)));
    }

    private boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey("online:" + userId));
    }

}
