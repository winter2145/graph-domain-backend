package com.xin.graphdomainbackend.infrastructure.websocket.picture.handler;

import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditRequestMessage;
import com.xin.graphdomainbackend.user.dao.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;


/**
 * 图片编辑处理模板
 */
@Slf4j
public abstract class PictureEditHandlerTemplate {

    @Resource
    protected PictureEditBroadcastUtil broadcastUtil;

    @Resource
    protected PictureEditSessionService pictureEditSessionService;

    public abstract void handle(PictureEditRequestMessage message,
                                WebSocketSession session,
                                User user,
                                Long pictureId) throws Exception;
}
