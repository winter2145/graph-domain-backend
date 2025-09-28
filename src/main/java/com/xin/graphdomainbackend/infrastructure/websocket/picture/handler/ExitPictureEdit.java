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
 * 退出图片编辑 处理器
 */
@Component
public class ExitPictureEdit extends PictureEditHandlerTemplate{

    @Resource
    private UserService userService;

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 正在编辑的用户
        Long editingUserId = pictureEditSessionService.getEditingUserId(pictureId);
        // 确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除用户正在编辑该图片
            pictureEditSessionService.removeEditingUser(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastUtil.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }
}
