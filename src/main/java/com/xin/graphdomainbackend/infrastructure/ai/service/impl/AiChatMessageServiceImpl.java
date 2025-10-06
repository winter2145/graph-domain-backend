package com.xin.graphdomainbackend.infrastructure.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;
import com.xin.graphdomainbackend.infrastructure.ai.service.AiChatMessageService;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【ai_chat_message(AI聊天消息表，存储用户与AI的对话记录)】的数据库操作Service实现
* @createDate 2025-10-05 10:48:14
*/
@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
    implements AiChatMessageService{

}




