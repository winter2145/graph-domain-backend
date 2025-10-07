package com.xin.graphdomainbackend.aidraw.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.aidraw.api.dto.request.AiDrawQueryRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatSessionVO;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatSession;

import java.util.List;

/**
* @author Administrator
* @description 针对表【ai_chat_session(AI聊天会话表，记录用户的聊天会话信息)】的数据库操作Service
* @createDate 2025-10-05 10:48:14
*/
public interface AiChatSessionService extends IService<AiChatSession> {

    /**
     * 创建会话
     */
    Long createSession(String userId, String title);


    /**
     * 获取某用户的会话列表
     */
    List<AiChatSessionVO> getUserSessions(String userId);

    /**
     * 分页获取某用户的会话列表
     */
    Page<AiChatSessionVO> getUserSessionsByPage(AiDrawQueryRequest aiDrawQueryRequest);

    /**
     * 更新会话标题
     */
    Boolean updateSessionTitle(Long sessionId, String title);


}
