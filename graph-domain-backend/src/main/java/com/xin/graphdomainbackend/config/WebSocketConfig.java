package com.xin.graphdomainbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xin.graphdomainbackend.Interceptor.WsHandshakeInterceptor;
import com.xin.graphdomainbackend.manager.websocket.chat.ChatWebSocketHandler;
import com.xin.graphdomainbackend.manager.websocket.picture.PictureEditHandler;
import com.xin.graphdomainbackend.manager.websocket.picture.disruptor.PictureEditEventWorkHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * WebSocket 配置（定义连接）
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    @Lazy
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*"); // 允许所有域连接，学习阶段方便调试

        // 添加图片协作配置
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
