package com.xin.graphdomainbackend.manager.websocket.picture;

import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.constant.WebSocketConstant;
import com.xin.graphdomainbackend.manager.websocket.picture.disruptor.PictureEditEventProducer;
import com.xin.graphdomainbackend.manager.websocket.picture.handler.PictureEditBroadcastUtil;
import com.xin.graphdomainbackend.manager.websocket.picture.handler.PictureEditHandlerTemplate;
import com.xin.graphdomainbackend.manager.websocket.picture.handler.PictureEditSessionService;
import com.xin.graphdomainbackend.model.dto.message.picture.PictureEditRequestMessage;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.model.vo.message.picture.PictureEditResponseMessage;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * 图片编辑 WebSocket 处理器
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private PictureEditHandlerFactory pictureEditHandlerFactory;

    @Resource
    private PictureEditSessionService pictureEditSessionService;

    @Resource
    private UserService userService;

    @Resource
    private PictureEditBroadcastUtil broadcastUtil;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 连接建立成功
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        // 获取用户和图片ID
        User user = (User) session.getAttributes().get(WebSocketConstant.USER);
        Long pictureId = (Long) session.getAttributes().get(WebSocketConstant.PICTURE_ID);

        // 保存会话到集合
        pictureEditSessionService.addSession(pictureId, session);

        // 广播给所有用户
        broadcastSystemMessage(pictureId, user, "加入编辑");
    }

    /**
     * 收到前端发送的消息，根据消息类别处理消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        // 获取消息内容, 将Json 转换为 PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get(WebSocketConstant.USER);
        Long pictureId = (Long) attributes.get(WebSocketConstant.PICTURE_ID);
        // 根据消息类型获取处理器
        PictureEditHandlerTemplate handlerTemplate = pictureEditHandlerFactory.getHandler(pictureEditRequestMessage.getType());

        if (handlerTemplate != null) {
            // 调用处理器
            // handlerTemplate.handle(pictureEditRequestMessage, session, user, pictureId);

            // 使用Disruptor 队列
            pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
        } else {
            log.warn("未知的消息类型: {}", pictureEditRequestMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取用户和图片ID
        User user = (User) session.getAttributes().get(WebSocketConstant.USER);
        Long pictureId = (Long) session.getAttributes().get(WebSocketConstant.PICTURE_ID);

        // 处理退出编辑
        PictureEditHandlerTemplate exitHandler = pictureEditHandlerFactory.getHandler(
                PictureEditMessageTypeEnum.EXIT_EDIT.getValue());

        if (exitHandler != null) {
            // 创建退出请求消息
            PictureEditRequestMessage exitRequest = new PictureEditRequestMessage();
            exitRequest.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());

            // 调用退出处理器
            exitHandler.handle(exitRequest, session, user, pictureId);
        }

        // 移除会话
        pictureEditSessionService.removeSession(pictureId, session);

        // 通知其他用户
        broadcastSystemMessage(pictureId, user, "离开编辑");
    }


    // 辅助方法：广播系统消息
    private void broadcastSystemMessage(Long pictureId, User user, String action) throws IOException {
        PictureEditResponseMessage response = new PictureEditResponseMessage();
        response.setType(PictureEditMessageTypeEnum.INFO.getValue());
        response.setMessage(String.format("用户 %s %s", user.getUserName(), action));
        response.setUser(userService.getUserVO(user));

        // 使用 工具类 的广播方法
        broadcastUtil.broadcastToPicture(pictureId, response);
    }
}
















