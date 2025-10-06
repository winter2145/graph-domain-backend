package com.xin.graphdomainbackend.infrastructure.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatSession;
import com.xin.graphdomainbackend.infrastructure.ai.service.AiChatSessionService;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatSessionMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【ai_chat_session(AI聊天会话表，记录用户的聊天会话信息)】的数据库操作Service实现
* @createDate 2025-10-05 10:48:14
*/
@Service
public class AiChatSessionServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession>
    implements AiChatSessionService{

}




