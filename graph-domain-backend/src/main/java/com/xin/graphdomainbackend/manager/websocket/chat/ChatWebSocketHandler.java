package com.xin.graphdomainbackend.manager.websocket.chat;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.constant.WebSocketConstant;
import com.xin.graphdomainbackend.manager.websocket.chat.disruptor.ChatEventProducer;
import com.xin.graphdomainbackend.model.dto.message.chat.ChatHistoryMessageRequest;
import com.xin.graphdomainbackend.model.dto.message.chat.ChatMessageSendRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatMessageVO;
import com.xin.graphdomainbackend.service.ChatMessageService;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Lazy
    private ChatEventProducer chatEventProducer;

    // 在线用户登记簿 1:1
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 图片聊天室会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // 空间聊天室会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> spaceSessions = new ConcurrentHashMap<>();

    // 私聊会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> privateChatSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        // 获取参数
        User user = (User) session.getAttributes().get(WebSocketConstant.USER);
        Long spaceId = (Long) session.getAttributes().get(WebSocketConstant.SPACE_ID);
        Long pictureId = (Long) session.getAttributes().get(WebSocketConstant.PICTURE_ID);
        Long privateChatId = (Long) session.getAttributes().get(WebSocketConstant.PRIVATE_CHAT_ID);

        if (user == null) {
            session.close();
            log.error("用户未登录");
            return;
        }

        try {
            // 保存用户session
            userSessions.put(user.getId(), session);

            // 图片聊天室
            if (pictureId != null) {
                // value = pictureSessions.get(pictureId),
                // value不存在，则将session存入set集合中
                // 使用set能去重，查少
                pictureSessions.computeIfAbsent(pictureId, value -> ConcurrentHashMap.newKeySet())
                        .add(session);
                // 查看历史消息
                sendPictureChatHistory(session, pictureId);
                // 广播在线用户信息
                broadcastOnlineUsers(pictureId, null, null);
            }
            // 私人聊天
            else if (privateChatId != null) {
                privateChatSessions.computeIfAbsent(privateChatId, value -> ConcurrentHashMap.newKeySet())
                        .add(session);
                sendPrivateChatHistory(session, privateChatId);
                broadcastOnlineUsers(null, null, privateChatId);
            }
            // 空间聊天
            else if (spaceId != null) {
                //  检查用户是否属于该空间
                if (!spaceUserService.isSpaceMember(user.getId(), spaceId)) {
                    log.error("用户不是空间成员");
                    session.close();
                    return; // 终止后续逻辑
                }
                // 属于
                spaceSessions.computeIfAbsent(spaceId, value -> new ConcurrentHashSet<>()).
                        add(session);
                sendSpaceChatHistory(session, spaceId);
                broadcastOnlineUsers(null, spaceId, null);
            }
        } catch (Exception e) {
            session.close();
            log.error("WebSocket连接建立失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // 1. 反序列化请求类 json -> Java
            ChatMessageSendRequest request = webSocketObjectMapper.readValue(message.getPayload(), ChatMessageSendRequest.class);
            User user = (User) session.getAttributes().get(WebSocketConstant.USER);
            if (user == null) {
                sendErrorMessage(session, "未登录");
                return;
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                sendErrorMessage(session, "消息内容不能为空");
                return;
            }

            // 2.构建ChatMessage 实体
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(request.getContent());
            chatMessage.setSenderId(user.getId());
            chatMessage.setCreateTime(new Date());
            chatMessage.setPictureId(request.getPictureId());
            chatMessage.setSpaceId(request.getSpaceId());
            chatMessage.setPrivateChatId(request.getPrivateChatId());
            chatMessage.setReplyId(request.getReplyId());
            chatMessage.setRootId(request.getRootId());

            // 3.判断消息类型和目标ID
            Integer messageType = null;
            Long targetId = null;
            if (request.getPrivateChatId() != null) {
                messageType = 1;
                targetId = request.getPrivateChatId();
            } else if (request.getPictureId() != null) {
                messageType = 2;
                targetId = request.getPictureId();
            } else if (request.getSpaceId() != null) {
                messageType = 3;
                targetId = request.getSpaceId();
            } else {
                sendErrorMessage(session, "无法确定消息类型");
                return;
            }
            chatMessage.setType(messageType);

            // 4. 使用disruptor 环形队列 异步处理 事件
            chatEventProducer.publishEvent(chatMessage, session, user, targetId, messageType);
        } catch (IOException e) {
            log.error("处理WebSocket消息失败", e);
            sendErrorMessage(session, "消息处理失败");
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        Long spaceId = (Long) session.getAttributes().get("spaceId");
        Long privateChatId = (Long) session.getAttributes().get("privateChatId");

        // 移除用户session
        if (user != null) {
            userSessions.remove(user.getId());
        }

        // 如果是私聊，移除私聊session
        if (privateChatId != null) {
            Set<WebSocketSession> sessions = privateChatSessions.get(privateChatId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    privateChatSessions.remove(privateChatId);

                } else {
                    broadcastOnlineUsers(null, null, privateChatId);
                }
            }
        }
        // 如果是图片聊天室，移除图片session
        else if (pictureId != null) {
            Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    pictureSessions.remove(pictureId);
                } else {
                    broadcastOnlineUsers(pictureId, null, null);
                }
            }
        }
        // 如果是空间聊天，移除空间session
        else if (spaceId != null) {
            Set<WebSocketSession> sessions = spaceSessions.get(spaceId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    spaceSessions.remove(spaceId);
                } else {
                    broadcastOnlineUsers(null, spaceId, null);
                }
            }
        }
    }

    /**
     *获取空间聊天的历史消息
     */
    private void sendSpaceChatHistory(WebSocketSession session, Long spaceId) {
        try {
            ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
            Long size = chatHistoryMessageRequest.getSize();
            Long current = chatHistoryMessageRequest.getCurrent();
            // 获取空间历史消息VO
            ChatHistoryPageResponse history = chatMessageService.getSpaceChatHistoryVO(spaceId, current, size);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(history)));
        } catch (Exception e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     *获取私人聊天的历史消息
     */
    private void sendPrivateChatHistory(WebSocketSession session, Long privateChatId) {
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

    /**
     *获取图片聊天室的历史消息
     */
    private void sendPictureChatHistory(WebSocketSession session, Long pictureId) {
        try {
            ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
            Long size = chatHistoryMessageRequest.getSize();
            Long current = chatHistoryMessageRequest.getCurrent();
            // 获取图片聊天室历史消息VO
            ChatHistoryPageResponse history = chatMessageService.getPictureChatHistoryVO(pictureId, current, size);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(history)));
        } catch (Exception e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     * 根据场景广播在线用户信息
     * @param pictureId 图片id
     * @param spaceId 空间id
     * @param privateChatId 私人聊天id
     */
    private void broadcastOnlineUsers(Long pictureId, Long spaceId, Long privateChatId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", WebSocketConstant.ONLINE_USERS);

            Set<WebSocketSession> targetSessions = null;
            if (pictureId != null) {
                targetSessions = pictureSessions.get(pictureId);

                // 获取该图片聊天室的在线用户
                Set<User> onlineUsers  = getOnlineUsers(targetSessions);
                response.put(WebSocketConstant.ONLINE_COUNT, onlineUsers.size());
                response.put(WebSocketConstant.ONLINE_USERS, onlineUsers);
                response.put(WebSocketConstant.PICTURE_ID, pictureId);
            } else if (privateChatId != null) {
                targetSessions = spaceSessions.get(privateChatId);

                //获取该空间内的在线用户
                Set<User> onlineUsers  = getOnlineUsers(targetSessions);
                response.put(WebSocketConstant.ONLINE_COUNT, onlineUsers.size());
                response.put(WebSocketConstant.ONLINE_USERS, onlineUsers);
                response.put(WebSocketConstant.SPACE_ID, spaceId);
            } else if (spaceId != null) {
                targetSessions = spaceSessions.get(spaceId);

                //获取该空间内的在线用户
                Set<User> onlineUsers  = getOnlineUsers(targetSessions);
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
                response.put(WebSocketConstant.PRIVATE_CHAT_ID, privateChatId);
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
     * 根据session列表 获取 用户列表
     * @param targetSessions Set session 集合
     * @return
     */
    private Set<User> getOnlineUsers(Set<WebSocketSession> targetSessions) {
        if (targetSessions != null) {
            return targetSessions.stream()
                    .map(s -> (User) s.getAttributes().get(WebSocketConstant.USER))
                    .filter(Objects::nonNull)
                    .map(this::createSafeUser)
                    .collect(Collectors.toSet());
        }
        return Collections.EMPTY_SET;
    }

    private User createSafeUser(User user) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUserAccount(user.getUserAccount());
        safeUser.setUserName(user.getUserName());
        safeUser.setUserAvatar(user.getUserAvatar());
        safeUser.setUserProfile(user.getUserProfile());
        safeUser.setUserRole(user.getUserRole());
        safeUser.setCreateTime(user.getCreateTime());
        return safeUser;
    }

    /**
     * 处理私聊消息
     */
    public void handlePrivateChatMessage(ChatMessage chatMessage, WebSocketSession session)
            throws IOException {
        User user = (User) session.getAttributes().get(WebSocketConstant.USER);
        // 保存消息
        chatMessageService.save(chatMessage);
        // 登录消息后清楚缓存
        deleteRedis(chatMessage);

        // 发送消息
        try {
            sendToPrivateChat(chatMessage);
        } catch (IOException e) {
            log.error("处理私聊消息失败: {}", e.getMessage());
            try {
                sendErrorMessage(session, e.getMessage());
            } catch (IOException ioException) {
                log.error("处理私聊消息失败", e);
                sendErrorMessage(session, "消息发送失败");
            }
        }
    }

    /**
     * 处理图片聊天室消息
     */
    public void handlePictureChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            // 保存消息
            chatMessageService.save(chatMessage);
            // 登录消息后清楚缓存
            deleteRedis(chatMessage);

            // 发送消息
            sendToPictureRoom(chatMessage);
        } catch (Exception e) {
            log.error("处理图片聊天室消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 处理空间聊天消息
     */
    public void handleSpaceChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            // 保存消息
            chatMessageService.save(chatMessage);
            // 登录消息后清楚缓存
            deleteRedis(chatMessage);

            // 发送消息
            sendToSpaceRoom(chatMessage);
        } catch (Exception e) {
            log.error("处理空间聊天消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 错误消息信息
     */
    private void sendErrorMessage(WebSocketSession session, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("message", message);
        session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(errorResponse)));
    }

    /**
     * 清理缓存
     */
    public void deleteRedis(ChatMessage message) {
        long id = 0;
        ChatHistoryMessageRequest chatHistoryMessageRequest = new ChatHistoryMessageRequest();
        Long size = chatHistoryMessageRequest.getSize();
        Long current = chatHistoryMessageRequest.getCurrent();
        if (message.getPictureId() != null) {
            id = message.getPictureId();
        } else if (message.getPrivateChatId() != null) {
            id = message.getPrivateChatId();
        } else if (message.getSpaceId() != null) {
            id = message.getSpaceId();
        }
        String cacheKey = RedisConstant.PICTURE_CHAT_HISTORY_PREFIX
                + id + ":" + current + ":" + size;
        stringRedisTemplate.delete(cacheKey);
    }

    /**
     * 发送私聊消息
     */
    private void sendToPrivateChat(ChatMessage message) throws IOException {
        if (message.getPrivateChatId() == null) {
            log.error("privateChatId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> sessions = privateChatSessions.get(message.getPrivateChatId());
        if (sessions != null && !sessions.isEmpty()) {
            ChatMessageVO vo = convertToVO(message);
            String messageStr = webSocketObjectMapper.writeValueAsString(vo);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送图片聊天室消息
     */
    private void sendToPictureRoom(ChatMessage chatMessage)  throws IOException {
        if (chatMessage.getPictureId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> sessions = pictureSessions.get(chatMessage.getPictureId());
        if (sessions != null && !sessions.isEmpty()) {
            ChatMessageVO vo = convertToVO(chatMessage);
            String messageStr = webSocketObjectMapper.writeValueAsString(vo);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送空间聊天消息
     */
    private void sendToSpaceRoom(ChatMessage chatMessage)  throws IOException {
        if (chatMessage.getSpaceId() == null) {
            log.error("spaceId为空，无法发送消息");
            return;
        }
        Set<WebSocketSession> sessions = spaceSessions.get(chatMessage.getSpaceId());
        if (sessions != null && !sessions.isEmpty()) {
            ChatMessageVO vo = convertToVO(chatMessage);
            String messageStr = webSocketObjectMapper.writeValueAsString(vo);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
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
}
