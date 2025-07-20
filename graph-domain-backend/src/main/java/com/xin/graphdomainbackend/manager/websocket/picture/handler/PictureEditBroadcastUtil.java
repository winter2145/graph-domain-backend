package com.xin.graphdomainbackend.manager.websocket.picture.handler;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.model.vo.message.picture.PictureEditResponseMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;

@Component
public class PictureEditBroadcastUtil {
    
    @Resource
    private ObjectMapper objectMapper;
    
    @Resource
    private PictureEditSessionService sessionService;
    
    /**
     * 广播给该图片的所有用户（支持排除某个Session）
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage response,
                                  WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = sessionService.getSessionsByPictureId(pictureId);
        
        if (CollUtil.isNotEmpty(sessionSet)) {
            String message = objectMapper.writeValueAsString(response);
            TextMessage textMessage = new TextMessage(message);
            
            for (WebSocketSession session : sessionSet) {
                if (excludeSession != null && session.getId().equals(excludeSession.getId())) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }
    
    /**
     * 广播该图片的所有用户
     */
    public void broadcastToPicture(Long pictureId, PictureEditResponseMessage response) 
        throws IOException {
        broadcastToPicture(pictureId, response, null);
    }
}