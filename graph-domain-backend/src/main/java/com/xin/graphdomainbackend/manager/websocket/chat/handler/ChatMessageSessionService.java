package com.xin.graphdomainbackend.manager.websocket.chat.handler;

import cn.hutool.core.collection.ConcurrentHashSet;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天会话管理服务
 */
@Service
public class ChatMessageSessionService {

    // 在线用户登记簿 1:1
    // private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 图片聊天室会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // 空间聊天室会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> spaceSessions = new ConcurrentHashMap<>();

    // 私聊会话群组 1:N
    private static final Map<Long, Set<WebSocketSession>> privateChatSessions = new ConcurrentHashMap<>();

    // ========== 在线用户管理方法 ==========

    /**
     * 添加用户在线会话
     * @param userId 用户ID
     * @param session WebSocket会话
     *//*
    public void addUserSession(Long userId, WebSocketSession session) {
        userSessions.put(userId, session);
    }

    *//**
     * 移除用户在线会话
     * @param userId 用户ID
     *//*
    public void removeUserSession(Long userId) {
        userSessions.remove(userId);
    }

    *//**
     * 获取用户会话
     * @param userId 用户ID
     * @return 用户的WebSocket会话，如果不存在则返回null
     *//*
    public WebSocketSession getUserSession(Long userId) {
        return userSessions.get(userId);
    }*/

    // ========== 图片聊天室管理方法 ==========

    /**
     * 添加会话到图片聊天室
     * @param pictureId 图片ID
     * @param session WebSocket会话
     */
    public void addPictureSession(Long pictureId, WebSocketSession session) {
        // value = pictureSessions.get(pictureId),
        // value不存在，则将session存入set集合中
        pictureSessions.computeIfAbsent(pictureId, value -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * 从图片聊天室移除会话
     * @param pictureId 图片ID
     * @param session 要移除的WebSocket会话
     */
    public void removePictureSession(Long pictureId, WebSocketSession session) {
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }

    /**
     * 获取图片聊天室的所有会话
     * @param pictureId 图片ID
     * @return 该图片聊天室的所有会话集合，如果不存在则返回null
     */
    public Set<WebSocketSession> getPictureSessions(Long pictureId) {
        return pictureSessions.get(pictureId);
    }

    // ========== 空间聊天室管理方法 ==========

    /**
     * 添加会话到空间聊天室
     * @param spaceId 空间ID
     * @param session WebSocket会话
     */
    public void addSpaceSession(Long spaceId, WebSocketSession session) {
        spaceSessions.computeIfAbsent(spaceId, value -> new ConcurrentHashSet<>()).
                add(session);
    }

    /**
     * 从空间聊天室移除会话
     * @param spaceId 空间ID
     * @param session 要移除的WebSocket会话
     */
    public void removeSpaceSession(Long spaceId, WebSocketSession session) {
        Set<WebSocketSession> sessions = spaceSessions.get(spaceId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                spaceSessions.remove(spaceId);
            }
        }
    }

    /**
     * 获取空间聊天室的所有会话
     * @param spaceId 空间ID
     * @return 该空间聊天室的所有会话集合，如果不存在则返回null
     */
    public Set<WebSocketSession> getSpaceSessions(Long spaceId) {
        return spaceSessions.get(spaceId);
    }

    // ========== 私聊会话管理方法 ==========

    /**
     * 添加会话到私聊房间
     * @param privateChatId 私聊Id
     * @param session WebSocket会话
     */
    public void addPrivateChatSession(Long privateChatId, WebSocketSession session) {
        privateChatSessions.computeIfAbsent(privateChatId, value -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * 从私聊房间移除会话
     * @param privateChatId 私聊Id
     * @param session 要移除的WebSocket会话
     */
    public void removePrivateChatSession(Long privateChatId, WebSocketSession session) {
        Set<WebSocketSession> sessions = privateChatSessions.get(privateChatId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                privateChatSessions.remove(privateChatId);
            }
        }
    }

    /**
     * 获取私聊房间的所有会话
     * @param privateChatId 私聊房间ID
     * @return 该私聊房间的所有会话集合，如果不存在则返回null
     */
    public Set<WebSocketSession> getPrivateChatSessions(Long privateChatId) {
        if (privateChatId == null) {
            return null;
        }
        return privateChatSessions.get(privateChatId);
    }

    // ========== 通用方法 ==========

/*    *//**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 如果在线返回true，否则返回false
     *//*
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }*/

}
