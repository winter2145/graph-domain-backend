package com.xin.graphdomainbackend.infrastructure.websocket.picture;

import com.xin.graphdomainbackend.infrastructure.websocket.picture.enums.PictureEditMessageTypeEnum;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.handler.EditActionPictureEdit;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.handler.EnterPictureEdit;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.handler.ExitPictureEdit;
import com.xin.graphdomainbackend.infrastructure.websocket.picture.handler.PictureEditHandlerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PictureEditHandlerFactory {

    private final Map<String, PictureEditHandlerTemplate> handlers = new HashMap<>();

    @Autowired
    public PictureEditHandlerFactory(List<PictureEditHandlerTemplate> handlerList) {
        for (PictureEditHandlerTemplate handler : handlerList) {
            if (handler instanceof EnterPictureEdit) {
                handlers.put(PictureEditMessageTypeEnum.ENTER_EDIT.getValue(), handler);
            } else if (handler instanceof EditActionPictureEdit) {
                handlers.put(PictureEditMessageTypeEnum.EDIT_ACTION.getValue(), handler);
            } else if (handler instanceof ExitPictureEdit) {
                handlers.put(PictureEditMessageTypeEnum.EXIT_EDIT.getValue(), handler);
            }
        }
    }

    public PictureEditHandlerTemplate getHandler(String messageType) {
        return handlers.get(messageType);
    }
}
