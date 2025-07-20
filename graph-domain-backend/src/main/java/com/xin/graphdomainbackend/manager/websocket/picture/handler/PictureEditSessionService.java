package com.xin.graphdomainbackend.manager.websocket.picture.handler;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑会话管理服务
 */
@Service
public class PictureEditSessionService {

    // 每张图片的编辑状态， key：pictureId, value: 当前正在编辑的用户
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存搜索连接的会话， key：pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 添加一个 WebSocket 会话到指定图片的会话集合中
     *
     * @param pictureId 图片 ID
     * @param session   当前用户的 WebSocket 会话
     */
    public void addSession(Long pictureId, WebSocketSession session) {
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
    }

    /**
     * 从指定图片的会话集合中移除一个 WebSocket 会话
     *
     * @param pictureId 图片 ID
     * @param session   要移除的 WebSocket 会话
     */
    public void removeSession(Long pictureId, WebSocketSession session) {
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }

    /**
     * 获取指定图片当前所有连接的 WebSocket 会话
     *
     * @param pictureId 图片 ID
     * @return 当前连接到该图片的所有会话集合
     */
    public Set<WebSocketSession> getSessionsByPictureId(Long pictureId) {
        return pictureSessions.get(pictureId);
    }

    /**
     * 获取当前正在编辑指定图片的用户 ID
     *
     * @param pictureId 图片 ID
     * @return 编辑该图片的用户 ID，如果没有人编辑则返回 null
     */
    public Long getEditingUserId(Long pictureId) {
        return pictureEditingUsers.get(pictureId);
    }

    /**
     * 设置某个用户为正在编辑指定图片的用户
     *
     * @param pictureId 图片 ID
     * @param userId    用户 ID
     */
    public void setEditingUser(Long pictureId, Long userId) {
        pictureEditingUsers.put(pictureId, userId);
    }

    /**
     * 移除指定图片的编辑用户信息，表示没有用户在编辑该图片
     *
     * @param pictureId 图片 ID
     */
    public void removeEditingUser(Long pictureId) {
        pictureEditingUsers.remove(pictureId);
    }
}
