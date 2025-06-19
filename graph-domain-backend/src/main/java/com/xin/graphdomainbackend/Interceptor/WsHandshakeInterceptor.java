package com.xin.graphdomainbackend.Interceptor;

import cn.hutool.core.util.StrUtil;
import com.xin.graphdomainbackend.constant.WebSocketConstant;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * WebSocket 拦截器，建立连接前要先校验
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    // 建立连接前先校验
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 1. 类型检查确保是Servlet环境
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }

        // 2. 获取原始HttpServletRequest
        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();


        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);

        // 获取参数
        String spaceIdStr = httpRequest.getParameter(WebSocketConstant.SPACE_ID);
        String privateChatIdStr =httpRequest.getParameter(WebSocketConstant.PRIVATE_CHAT_ID);
        String pictureIdStr = httpRequest.getParameter(WebSocketConstant.PICTURE_ID);

        // 设置用户信息
        attributes.put(WebSocketConstant.USER, loginUser);

        // 存在pictureId,则设置
        if (StrUtil.isNotBlank(pictureIdStr)) {
            long pictureId = Long.parseLong(pictureIdStr);
            attributes.put(WebSocketConstant.PICTURE_ID, pictureId);
        }

        // 存在spaceId,则设置
        if (StrUtil.isNotBlank(spaceIdStr)) {
            long spaceId = Long.parseLong(spaceIdStr);
            attributes.put(WebSocketConstant.SPACE_ID, spaceId);
        }

        // 存在privateChatId,则设置
        if (StrUtil.isNotBlank(privateChatIdStr)) {
            long privateChatId = Long.parseLong(privateChatIdStr);
            attributes.put(WebSocketConstant.PRIVATE_CHAT_ID, privateChatId);
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.info("WebSocket连接已建立: {}", request.getRemoteAddress());
    }

}
