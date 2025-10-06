package com.xin.graphdomainbackend.infrastructure.ai.service;

import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatMessageVO;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatSessionVO;

import java.util.List;

public interface AiDrawingService {

    /**
     * 创建新会话，返回 sessionId（前端调用后可以保存 sessionId）
     */
    Long createSession(String userId, String title);

    /**
     * 保存用户输入
     */
    Long saveUserMessage(String userId, Long sessionId, String userInput);

    /**
     * 优化 工程提示词
     */
    String optimizePrompt(String userId, Long sessionId, String prompt, Long roundId);

    /**
     * 生成图片（支持 conversationId = sessionId as string）
     * 返回 COS 最终 URL
     */
    String generateImage(String userId, Long sessionId, String realPrompt);

    /**
     * 获取会话历史（按时间升序）
     */
    List<AiChatMessageVO> getSessionHistoryMessages(Long sessionId);

    /**
     * 获取某用户的会话列表
     */
    List<AiChatSessionVO> getUserSessions(String userId);

    /**
     * 更新会话标题
     */
    Boolean updateSessionTitle(Long sessionId);
}
