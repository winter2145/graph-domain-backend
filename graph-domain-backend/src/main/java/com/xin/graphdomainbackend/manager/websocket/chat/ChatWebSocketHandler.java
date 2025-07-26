package com.xin.graphdomainbackend.manager.websocket.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.constant.WebSocketConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.websocket.chat.disruptor.ChatEventProducer;
import com.xin.graphdomainbackend.manager.websocket.chat.handler.ChatMessageBroadcastUtil;
import com.xin.graphdomainbackend.manager.websocket.chat.handler.ChatMessageHandlerTemplate;
import com.xin.graphdomainbackend.manager.websocket.chat.handler.ChatMessageSessionService;
import com.xin.graphdomainbackend.model.dto.message.chat.ChatMessageSendRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.ChatMessage;
import com.xin.graphdomainbackend.model.entity.websocket.PrivateChat;
import com.xin.graphdomainbackend.model.enums.ChatMessageTypeEnum;
import com.xin.graphdomainbackend.model.vo.message.chat.ChatHistoryPageResponse;
import com.xin.graphdomainbackend.service.ChatMessageService;
import com.xin.graphdomainbackend.service.PrivateChatService;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserFollowsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

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
    @Lazy
    private ChatEventProducer chatEventProducer;

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    private UserFollowsService userFollowsService;

    @Resource
    private ChatMessageSessionService sessionService;

    @Resource
    private ChatMessageBroadcastUtil broadcastUtil;

    @Resource
    private ChatMessageHandlerFactory chatMessageHandlerFactory;


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
            // 保存用户session - 这里是全局用户会话记录，用于判断用户是否在线
            // sessionService.addUserSession(user.getId(), session);

            // 图片聊天室
            if (pictureId != null) {
                // 添加会话到图片聊天室
                sessionService.addPictureSession(pictureId, session);
                // 查看图片历史消息
                ChatMessageHandlerTemplate handler = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.PICTURE.getValue());
                handler.sendChatHistory(pictureId, session);

                // 广播在线用户信息
                broadcastUtil.broadcastOnlineUsers(pictureId, null, null);
            }
            // 私人聊天
            else if (privateChatId != null) {
                // 添加会话到私人聊天室
                sessionService.addPrivateChatSession(privateChatId, session);
                // 查看私人历史消息
                ChatMessageHandlerTemplate handler = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.PRIVATE.getValue());
                handler.sendChatHistory(privateChatId, session);
                // 广播在线用户信息
                broadcastUtil.broadcastOnlineUsers(null, null, privateChatId);
            }
            // 空间聊天
            else if (spaceId != null) {
                //  检查用户是否属于该空间
                if (!spaceUserService.isSpaceMember(user.getId(), spaceId)) {
                    log.error("用户不是空间成员");
                    session.close();
                    return; // 终止后续逻辑
                }
                // 属于该空间,添加会话到空间聊天室
                sessionService.addSpaceSession(spaceId, session);
                // 查看空间历史消息
                ChatMessageHandlerTemplate handler = chatMessageHandlerFactory.getHandler(ChatMessageTypeEnum.SPACE.getValue());
                handler.sendChatHistory(spaceId, session);
                // 广播在线用户信息
                broadcastUtil.broadcastOnlineUsers(null, spaceId, null);
            }
        } catch (Exception e) {
            session.close();
            log.error("WebSocket连接建立失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // 处理历史消息加载请求
            String payload = message.getPayload();
            if (payload != null && payload.contains("\"type\":\"loadMore\"")) {
                handleLoadMoreMessage(session, payload);
                return;
            }

            // 1. 反序列化请求类 json -> Java
            ChatMessageSendRequest request = webSocketObjectMapper.readValue(message.getPayload(), ChatMessageSendRequest.class);
            User user = (User) session.getAttributes().get(WebSocketConstant.USER);
            if (user == null) {
                broadcastUtil.sendErrorMessage(session, "未登录");
                return;
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                broadcastUtil.sendErrorMessage(session, "消息内容不能为空");
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

                /// 校验私聊权限（未互关只能发一条）
                PrivateChat privateChat = privateChatService.getById(targetId);
                if (privateChat == null) {
                    broadcastUtil.sendErrorMessage(session, "私聊不存在");
                    return;
                }

                // 检查权限
                if (!checkPrivateChatPermission(user.getId(), privateChat, session)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "对方未关注前，只能发送一条消息");
                }
                // 这里推断 receiverId
                Long receiverId;
                if (privateChat.getUserId().equals(user.getId())) {
                    receiverId = privateChat.getTargetUserId();
                } else {
                    receiverId = privateChat.getUserId();
                }
                chatMessage.setReceiverId(receiverId);

            } else if (request.getPictureId() != null) {
                messageType = 2;
                targetId = request.getPictureId();
            } else if (request.getSpaceId() != null) {
                messageType = 3;
                targetId = request.getSpaceId();
            } else {
                broadcastUtil.sendErrorMessage(session, "无法确定消息类型");
                return;
            }
            chatMessage.setType(messageType);

            // 4. 使用disruptor 环形队列 异步处理 事件
            chatEventProducer.publishEvent(chatMessage, session, user, targetId, messageType);
        } catch (IOException e) {
            log.error("处理WebSocket消息失败", e);
            broadcastUtil.sendErrorMessage(session, "消息处理失败");
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        Long spaceId = (Long) session.getAttributes().get("spaceId");
        Long privateChatId = (Long) session.getAttributes().get("privateChatId");

        // 从特定聊天室中移除会话，保留全局会话
        if (privateChatId != null) {
            Set<WebSocketSession> sessions = sessionService.getPrivateChatSessions(privateChatId);
            sessionService.removePrivateChatSession(privateChatId, session);
            if (!sessions.isEmpty()) {
                broadcastUtil.broadcastOnlineUsers(null, null, privateChatId);
            }
        }
        // 如果是图片聊天室，移除图片session
        else if (pictureId != null) {
            Set<WebSocketSession> sessions = sessionService.getPictureSessions(pictureId);
            sessionService.removePictureSession(pictureId, session);
            if (!sessions.isEmpty()) {
                broadcastUtil.broadcastOnlineUsers(pictureId, null, null);
            }
        }
        // 如果是空间聊天，移除空间session
        else if (spaceId != null) {
            Set<WebSocketSession> sessions = sessionService.getSpaceSessions(spaceId);
            sessionService.removeSpaceSession(spaceId, session);
            if (!sessions.isEmpty()) {
                broadcastUtil.broadcastOnlineUsers(null, spaceId, null);
            }
        }
    }


    /**
     * 处理加载更多历史消息的请求
     */
    private void handleLoadMoreMessage(WebSocketSession session, String payload) throws IOException {
        try {
            JsonNode jsonNode = webSocketObjectMapper.readTree(payload);
            Long privateChatId = jsonNode.has("privateChatId") ? jsonNode.get("privateChatId").asLong() : null;
            Long pictureId = jsonNode.has("pictureId") ? jsonNode.get("pictureId").asLong() : null;
            Long spaceId = jsonNode.has("spaceId") ? jsonNode.get("spaceId").asLong() : null;
            int page = jsonNode.has("page") ? jsonNode.get("page").asInt() : 1;
            int pageSize = jsonNode.has("pageSize") ? jsonNode.get("pageSize").asInt() : 20;

            ChatHistoryPageResponse history = null;

            if (privateChatId != null) {
                history = chatMessageService.getPrivateChatHistoryVO(privateChatId, (long) page, (long) pageSize);
            } else if (pictureId != null) {
                history = chatMessageService.getPictureChatHistoryVO(pictureId, (long) page, (long) pageSize);
            } else if (spaceId != null) {
                history = chatMessageService.getSpaceChatHistoryVO(spaceId, (long) page, (long) pageSize);
            }

            if (history != null) {
                session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(history)));
            }
        } catch (Exception e) {
            log.error("加载历史消息失败", e);
            broadcastUtil.sendErrorMessage(session, "加载历史消息失败");
        }
    }

    /**
     * 校验私聊权限（未互关只能发一条）
     * @return 是否允许发送
     */
    private boolean checkPrivateChatPermission(Long senderId, PrivateChat privateChat, WebSocketSession session) throws IOException {
        try {
            // 确定接收者ID
            Long receiverId;
            if (privateChat.getUserId().equals(senderId)) {
                receiverId = privateChat.getTargetUserId();
            } else if (privateChat.getTargetUserId().equals(senderId)) {
                receiverId = privateChat.getUserId();
            } else {
                broadcastUtil.sendErrorMessage(session, "您不是该私聊的参与者");
                return false;
            }

            // 检查是否互关
            boolean isMutual = userFollowsService.isMutualRelations(senderId, receiverId);
            if (!isMutual) {
                // 检查是否已经发送过消息
                boolean hasSent = chatMessageService.hasSentMessage(senderId, receiverId);
                if (hasSent) {
                    broadcastUtil.sendErrorMessage(session, "对方未关注前，只能发送一条消息");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("校验私聊权限失败", e);
            broadcastUtil.sendErrorMessage(session, "校验权限失败");
            return false;
        }
    }


}
