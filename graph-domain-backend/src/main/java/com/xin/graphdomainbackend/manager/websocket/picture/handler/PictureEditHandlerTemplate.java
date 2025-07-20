package com.xin.graphdomainbackend.manager.websocket.picture.handler;

import com.xin.graphdomainbackend.model.dto.message.picture.PictureEditRequestMessage;
import com.xin.graphdomainbackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

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
