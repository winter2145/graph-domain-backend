package com.xin.graphdomainbackend.manager.websocket.picture.handler;

import com.xin.graphdomainbackend.model.dto.message.picture.PictureEditRequestMessage;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.model.vo.message.picture.PictureEditResponseMessage;
import com.xin.graphdomainbackend.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

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
