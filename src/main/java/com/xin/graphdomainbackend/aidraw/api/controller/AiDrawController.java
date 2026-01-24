package com.xin.graphdomainbackend.aidraw.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.aidraw.api.dto.request.AiDrawQueryRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.request.CreateSessionRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.request.GenerateImageRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatMessageVO;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatSessionVO;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiGenerateImageVO;
import com.xin.graphdomainbackend.aidraw.service.AiChatMessageService;
import com.xin.graphdomainbackend.aidraw.service.AiChatSessionService;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.infrastructure.ai.constant.AiConstant;
import com.xin.graphdomainbackend.infrastructure.ai.service.AiDrawingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/ai_draw")
public class AiDrawController {

    @Resource
    private AiDrawingService drawingService;

    @Resource
    private AiChatMessageService messageService;

    @Resource
    private AiChatSessionService sessionService;

    /**
     * 创建会话
     */
    @PostMapping("/session/create")
    @LoginCheck
    public BaseResponse<Long> createSession(@Valid @RequestBody CreateSessionRequest createSessionRequest) {
        return ResultUtils.success(
                sessionService.createSession(createSessionRequest.getUserId(), createSessionRequest.getTitle())
        );
    }

    /**
     * 绘图
     */
    @PostMapping("/session/{sessionId}/message")
    @LoginCheck
    public BaseResponse<AiGenerateImageVO> generateImage(@PathVariable Long sessionId,
                                                         @Valid @RequestBody GenerateImageRequest req) {

        AiGenerateImageVO aiGenerateImageVO = new AiGenerateImageVO();

        String userId = req.getUserId();
        String prompt = req.getPrompt();

        // 1. 返回轮次ID
        Long roundId = drawingService.getRoundId(userId, sessionId, prompt);
        aiGenerateImageVO.setRole(AiConstant.USER_ROLE);

        // 2. AI 优化提示词
        String optimized = drawingService.optimizePrompt(userId, sessionId, prompt, roundId);
        aiGenerateImageVO.setRole(AiConstant.ASSISTANT_ROLE);

        // 3.判断是否绘图
        if (optimized.startsWith("AUTO_DRAW")) { // 绘图
            String realPrompt = optimized.substring("AUTO_DRAW".length()).trim();

            // 生成图片、更新 assistant 内容
            String cosUrl = drawingService.generateImage(userId, sessionId, realPrompt);
            aiGenerateImageVO.setType("image");
            aiGenerateImageVO.setCosUrl(cosUrl);
            aiGenerateImageVO.setContent("");
            return ResultUtils.success(aiGenerateImageVO); // 可返回 URL 供前端直接展示
        }
        aiGenerateImageVO.setType("text");
        aiGenerateImageVO.setContent(optimized);

        // 4. 继续追问文本
        return ResultUtils.success(aiGenerateImageVO);
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/session/{sessionId}/history_messages")
    @LoginCheck
    public BaseResponse<List<AiChatMessageVO>> getMessages(@PathVariable Long sessionId) {
        AiDrawQueryRequest aiDrawQueryRequest = new AiDrawQueryRequest();
        aiDrawQueryRequest.setSessionId(sessionId);
        return ResultUtils.success(messageService.getSessionHistoryMessages(sessionId));
    }

    /**
     * 获取会话历史消息 分页
     */
    @PostMapping("/session/{sessionId}/history_messages")
    @LoginCheck
    public BaseResponse<Page<AiChatMessageVO>> getMessagesByPage(@PathVariable Long sessionId,@RequestBody AiDrawQueryRequest aiDrawQueryRequest) {
        aiDrawQueryRequest.setSessionId(sessionId);
        return ResultUtils.success(messageService.getSessionsHistoryByPage(aiDrawQueryRequest));
    }

    /**
     * 获取用户会话列表近7条
     */
    @GetMapping("/sessions")
    @LoginCheck
    public BaseResponse<List<AiChatSessionVO>> getUserSessions(@RequestParam String userId) {
        return ResultUtils.success(sessionService.getUserSessions(userId));
    }

    /**
     * 更新会话名字
     */
    @PostMapping("/session/{sessionId}/update_title")
    @LoginCheck
    public BaseResponse<Boolean> updateSessionTitle(@PathVariable Long sessionId, @RequestParam String title) {
        return ResultUtils.success(sessionService.updateSessionTitle(sessionId, title));
    }

    /**
     * 删除 对话
     */
    @PostMapping("/delete/{sessionId}")
    @LoginCheck
    public BaseResponse<Boolean> deleteSession(@PathVariable Long sessionId) {
        return ResultUtils.success(sessionService.deleteSession(sessionId));
    }
}
