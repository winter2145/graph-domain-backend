package com.xin.graphdomainbackend.infrastructure.websocket.picture.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.PictureEditHandlerFactory;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.handler.PictureEditHandlerTemplate;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.model.PictureEditRequestMessage;
import com.xin.graphdomainbackend.user.dao.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;


/**
 * 图片编辑事件处理器（消费者）
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandlerFactory pictureEditHandlerFactory;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        // 获取到消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        String value = pictureEditMessageTypeEnum.getValue();

        // 使用模板 调用
        PictureEditHandlerTemplate templateHandler = pictureEditHandlerFactory.getHandler(value);
        templateHandler.handle(pictureEditRequestMessage, session, user, pictureId);
    }
}
