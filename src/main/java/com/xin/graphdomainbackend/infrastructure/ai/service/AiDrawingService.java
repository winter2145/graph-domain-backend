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
     * CF flux-1-schnell 模型
     * 生成图片（支持 conversationId = sessionId as string）
     * 返回 COS 最终 URL
     */
    String generateImage(String userId, Long sessionId, String realPrompt);

    /**
     * CF stable-diffusion-xl-base-1.0模型
     * 生成图片（支持 conversationId = sessionId as string）
     * 返回 COS 最终 URL
     */
    String generateImageByStable(String userId, Long sessionId, String realPrompt);

    /**
     * 阿里云百炼模型
     * 生成图片（支持 conversationId = sessionId as string）
     * 返回 COS 最终 URL
     */
    String generateImageByAliyun(String userId, Long sessionId, String realPrompt);


}
