package com.xin.graphdomainbackend.infrastructure.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatMessageVO;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatSessionVO;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatSession;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatMessageMapper;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatSessionMapper;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.infrastructure.ai.constant.AiConstant;
import com.xin.graphdomainbackend.infrastructure.ai.service.AiDrawingService;
import com.xin.graphdomainbackend.infrastructure.cos.model.UploadPictureResult;
import com.xin.graphdomainbackend.infrastructure.cos.upload.PictureUploadTemplate;
import com.xin.graphdomainbackend.infrastructure.cos.upload.UrlPictureUpload;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiDrawingServiceImpl implements AiDrawingService {

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private ChatClient chatClient;

    @Resource
    private ImageModel imageModel;

    @Resource
    private AiChatSessionMapper sessionMapper;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private AiChatMessageMapper messageMapper;


    @Override
    public Long createSession(String userId, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        sessionMapper.insert(session);
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveUserMessage(String userId, Long sessionId, String userInput) {
        // 鉴权
        LambdaQueryWrapper<AiChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiChatSession::getId, sessionId).eq(AiChatSession::getUserId, userId);
        boolean exists = sessionMapper.exists(queryWrapper);

        if (!exists) {
            throw new RuntimeException("当前用户会话不存在");
        }

        // 判断是否新绘图
        boolean isNewRound = isNewDrawingCommand(userInput);

        // 获取当前最大 roundId
        Long currentRoundId = messageMapper.findMaxRoundIdBySessionId(sessionId);
        if (currentRoundId == null) {
            currentRoundId = 1L;
        } else if (isNewRound) {
            currentRoundId += 1;
        }

        // 存用户消息
        try {
            AiChatMessage userMsg = new AiChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRoundId(currentRoundId);
            userMsg.setRole(AiConstant.USER_ROLE);
            userMsg.setContent(userInput);
            messageMapper.insert(userMsg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return currentRoundId; // 返回当前使用的roundId
    }


    @Override
    public String optimizePrompt(String userId, Long sessionId, String prompt, Long roundId) {

        String conversationId = sessionId + "_draw_" + roundId; // 会话 ID

        // 指定只保留最近6条消息（3轮对话）
        int lastN = 6;

        // 1. 使用 ChatClient 优化 Prompt（带 memory + logger advisor）
        String optimized = chatClient.prompt()
                .system(AiConstant.PROMPT)
                // .advisors(a -> a
                //         .param("chat_memory_conversation_id", conversationId)
                //         .param("chat_memory_last_n", lastN)
                // )
                .messages(chatMemory.get(conversationId, lastN))
                .user(prompt)
                .call()
                .content();

        // 保存优化后的信息
        saveAssistantMessage(sessionId, optimized, roundId);

        return optimized;   // 前端拿到先展示，等待用户确认
    }

    // 保存ai优化消息
    private void saveAssistantMessage(Long sessionId, String content, Long roundId) {
        AiChatMessage assistantMsg = new AiChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole(AiConstant.ASSISTANT_ROLE);
        assistantMsg.setContent(content);
        assistantMsg.setRoundId(roundId);
        messageMapper.insert(assistantMsg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateImage(String userId, Long sessionId, String realPrompt) {

        // 1. 取出最新一条 assistant 消息
        List<AiChatMessage> list = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .eq(AiChatMessage::getRole, AiConstant.ASSISTANT_ROLE)
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .last("LIMIT 1"));
        if (list.isEmpty()) {
            throw new RuntimeException("请先发送绘图需求");
        }
        AiChatMessage aiChatMessage = list.getFirst();
        Long currentMessageId = aiChatMessage.getId();

        // 2. 调用 ImageModel 生成图片
        ImagePrompt imagePrompt = new ImagePrompt(realPrompt,
                DashScopeImageOptions
                        .builder()
                        .withModel("qwen-image-plus")
                        .withN(1).build());

        ImageResponse resp = imageModel.call(imagePrompt);
        if (resp == null || resp.getResults() == null || resp.getResults().isEmpty()) {
            throw new RuntimeException("图像生成失败：没有返回结果");
        }
        String tmpImageUrl = resp.getResults().getFirst().getOutput().getUrl();

        // 3. 上传到 COS
        String uploadPathPrefix = "ai-drawings/" + userId + "/" + sessionId + "/";

        // 上传图片得到图片信息
        PictureUploadTemplate pictureUploadTemplate = urlPictureUpload;
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPictureResult(tmpImageUrl, uploadPathPrefix);
        String cosUrl = uploadPictureResult.getUrl().isEmpty() ?
                uploadPictureResult.getWebpUrl() : uploadPictureResult.getUrl();

        // 6. 更新 assistant 记录(把图片 URL、优化后的content)
        Boolean result = messageMapper.updateAssistantById(currentMessageId, realPrompt, cosUrl);
        if (!result) {
            throw new RuntimeException("图片URL 更新数据库失败");
        }

        return cosUrl;
    }

    @Override
    public List<AiChatMessageVO> getSessionHistoryMessages(Long sessionId) {
        List<AiChatMessage> aiChatMessages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreateTime)
        );
        if (aiChatMessages.isEmpty()) {
            return new ArrayList<>();
        }
        return aiChatMessages.stream().map(aiChatMessage -> {
            AiChatMessageVO aiChatMessageVO = new AiChatMessageVO();
            BeanUtil.copyProperties(aiChatMessage, aiChatMessageVO);
            if (StringUtils.hasText(aiChatMessage.getImageUrl())) {
                aiChatMessageVO.setContent(""); // 对于有图片的返回内容，只返回图片url
            }
            return aiChatMessageVO;
        }).toList();
    }

    @Override
    public List<AiChatSessionVO> getUserSessions(String userId) {
        List<AiChatSession> aiChatSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getCreateTime)
                        .last("LIMIT 7")
        );
        if (aiChatSessions.isEmpty()) {
            return new ArrayList<>();
        }

        return aiChatSessions.stream().map(aiChatSession -> {
            AiChatSessionVO aiChatSessionVO = new AiChatSessionVO();
            BeanUtil.copyProperties(aiChatSession, aiChatSessionVO);
            return aiChatSessionVO;
        }).toList();
    }

    @Override
    public Boolean updateSessionTitle(Long sessionId) {
        if (sessionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"sessionId 不能小于等于0");
        }
        return sessionMapper.updateTitle(sessionId);
    }

    /**
     * 判断是否是新的绘图需求
     */
    private boolean isNewDrawingCommand(String prompt) {
        if (prompt == null) return false;
        String p = prompt.trim();
        return AiConstant.NEW_DRAWING.stream().anyMatch(p::contains);
    }

}
