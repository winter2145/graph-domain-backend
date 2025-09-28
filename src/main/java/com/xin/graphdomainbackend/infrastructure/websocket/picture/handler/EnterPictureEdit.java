package com.xin.graphdomainbackend.infrastructure.websocket.picture.handler;

import com.xin.graphdomainbackend.infrastructure.websocket.picture.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditRequestMessage;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditResponseMessage;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;


/**
 * 进入图片编辑处理 处理器
 */
@Component
public class EnterPictureEdit extends PictureEditHandlerTemplate{

    @Resource
    private UserService userService;

    @Override
    public void handle(PictureEditRequestMessage message, WebSocketSession session, User user, Long pictureId) throws Exception {
        if (pictureEditSessionService.getEditingUserId(pictureId) == null) {
            pictureEditSessionService.setEditingUser(pictureId, user.getId());

            PictureEditResponseMessage response = new PictureEditResponseMessage();
            response.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            response.setMessage(String.format("用户 %s 开始编辑图片", user.getUserName()));
            response.setUser(userService.getUserVO(user));

            broadcastUtil.broadcastToPicture(pictureId, response);
        }
    }
}
