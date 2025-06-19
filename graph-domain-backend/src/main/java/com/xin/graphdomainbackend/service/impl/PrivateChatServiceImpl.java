package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.mapper.PrivateChatMapper;

import com.xin.graphdomainbackend.model.entity.websocket.PrivateChat;
import com.xin.graphdomainbackend.service.PrivateChatService;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【private_chat(私聊表)】的数据库操作Service实现
* @createDate 2025-06-12 18:25:02
*/
@Service
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatMapper, PrivateChat>
    implements PrivateChatService {

}




