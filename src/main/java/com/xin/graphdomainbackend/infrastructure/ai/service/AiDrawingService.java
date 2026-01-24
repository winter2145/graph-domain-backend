package com.xin.graphdomainbackend.infrastructure.ai.service;

public interface AiDrawingService {

    /**
     * 获取会话轮次
     */
    Long getRoundId(String userId, Long sessionId, String userInput);

    /**
     * 优化 工程提示词
     */
    String optimizePrompt(String userId, Long sessionId, String prompt, Long roundId);

    /**
     * 生成图片（支持 conversationId = sessionId as string）
     * 返回 COS 最终 URL
     */
    String generateImage(String userId, Long sessionId, String realPrompt);


}
