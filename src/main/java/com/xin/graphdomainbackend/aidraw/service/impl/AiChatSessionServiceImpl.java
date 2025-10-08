package com.xin.graphdomainbackend.aidraw.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.aidraw.api.dto.request.AiDrawQueryRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatSessionVO;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatSession;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatSessionMapper;
import com.xin.graphdomainbackend.aidraw.service.AiChatSessionService;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author Administrator
* @description 针对表【ai_chat_session(AI聊天会话表，记录用户的聊天会话信息)】的数据库操作Service实现
* @createDate 2025-10-05 10:48:14
*/
@Service
public class AiChatSessionServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession>
    implements AiChatSessionService {

    @Override
    public Long createSession(String userId, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        this.save(session);
        return session.getId();
    }

    @Override
    public List<AiChatSessionVO> getUserSessions(String userId) {
        List<AiChatSession> aiChatSessions = this.baseMapper.selectList(
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
    public Page<AiChatSessionVO> getUserSessionsByPage(AiDrawQueryRequest aiDrawQueryRequest) {
        ThrowUtils.throwIf(aiDrawQueryRequest.getUserId() == null, ErrorCode.PARAMS_ERROR);

        long current = aiDrawQueryRequest.getCurrent();
        long pageSize = aiDrawQueryRequest.getPageSize();

        LambdaQueryWrapper<AiChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiChatSession::getUserId, aiDrawQueryRequest.getUserId())
                .orderByDesc(AiChatSession::getCreateTime);
        // 分页查询
        Page<AiChatSession> page = this.page(new Page<>(current, pageSize), queryWrapper);

        // 创建一个同样大小的分页对象
        long total = page.getTotal();
        Page<AiChatSessionVO> aiChatSessionVOPage = new Page<>(current, pageSize, total);

        // 获取记录
        List<AiChatSession> records = page.getRecords();
        List<AiChatSessionVO> list = records.stream().map(aiChatSession -> {
            AiChatSessionVO aiChatSessionVO = new AiChatSessionVO();
            BeanUtil.copyProperties(aiChatSession, aiChatSessionVO);
            return aiChatSessionVO;
        }).toList();

        aiChatSessionVOPage.setRecords(list);

        return aiChatSessionVOPage;
    }

    @Override
    public Boolean updateSessionTitle(Long sessionId, String title) {
        if (sessionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"sessionId 不能小于等于0");
        }
        return this.baseMapper.updateTitle(sessionId, title);
    }
}




