package com.xin.graphdomainbackend.aidraw.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.aidraw.api.dto.request.AiDrawQueryRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatMessageVO;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;

import java.util.List;

/**
* @author Administrator
* @description 针对表【ai_chat_message(AI聊天消息表，存储用户与AI的对话记录)】的数据库操作Service
* @createDate 2025-10-05 10:48:14
*/
public interface AiChatMessageService extends IService<AiChatMessage> {

    /**
     * 获取会话历史（按时间升序）
     */
    List<AiChatMessageVO> getSessionHistoryMessages(Long sessionId);

    /**
     * 分页获取某用户的会话列表（按时间升序）
     */
    Page<AiChatMessageVO> getSessionsHistoryByPage(AiDrawQueryRequest aiDrawQueryRequest);
}
