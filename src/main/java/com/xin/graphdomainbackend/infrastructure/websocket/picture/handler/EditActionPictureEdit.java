package com.xin.graphdomainbackend.infrastructure.websocket.picture.handler;

import com.xin.graphdomainbackend.infrastructure.websocket.picture.enums.PictureEditActionEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditRequestMessage;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditResponseMessage;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 编辑图片 处理器
 */
@Component
@Slf4j
public class EditActionPictureEdit extends PictureEditHandlerTemplate{

    @Resource
    private UserService userService;

    @Override
    public void handle(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在编辑的用户
        Long editingUserId = pictureEditSessionService.getEditingUserId(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.error("无效的编辑动作");
            return;
        }
        // 确认是当前的编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构造响应，发送具体操作的通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s 执行 %s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastUtil.broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

}
