package com.xin.graphdomainbackend.aidraw.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.aidraw.api.dto.request.AiDrawQueryRequest;
import com.xin.graphdomainbackend.aidraw.api.dto.vo.AiChatMessageVO;
import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;
import com.xin.graphdomainbackend.aidraw.dao.mapper.AiChatMessageMapper;
import com.xin.graphdomainbackend.aidraw.service.AiChatMessageService;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
* @author Administrator
* @description 针对表【ai_chat_message(AI聊天消息表，存储用户与AI的对话记录)】的数据库操作Service实现
* @createDate 2025-10-05 10:48:14
*/
@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage>
    implements AiChatMessageService {

    @Override
    public List<AiChatMessageVO> getSessionHistoryMessages(Long sessionId) {
        List<AiChatMessage> aiChatMessages = this.baseMapper.selectList(
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
    public Page<AiChatMessageVO> getSessionsHistoryByPage(AiDrawQueryRequest aiDrawQueryRequest) {
        ThrowUtils.throwIf(aiDrawQueryRequest.getSessionId() == null, ErrorCode.PARAMS_ERROR);

        long current = aiDrawQueryRequest.getCurrent();
        long pageSize = aiDrawQueryRequest.getPageSize();

        LambdaQueryWrapper<AiChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiChatMessage::getSessionId, aiDrawQueryRequest.getSessionId())
                .orderByDesc(AiChatMessage::getCreateTime);

        Page<AiChatMessage> page = this.page(new Page<>(current, pageSize), queryWrapper);

        // 创建一个新的分页对象 一样大
        Page<AiChatMessageVO> aiChatMessageVOPage = new Page<>(current, pageSize, page.getTotal());

        // 塞入记录
        aiChatMessageVOPage.setRecords(
                page.getRecords()
                        .stream()
                        .map(aiChatMessage -> {
                            AiChatMessageVO aiChatMessageVO = new AiChatMessageVO();
                            BeanUtil.copyProperties(aiChatMessage, aiChatMessageVO);
                            if (StringUtils.hasText(aiChatMessage.getImageUrl())) {
                                aiChatMessageVO.setContent(""); // 对于有图片的返回内容，只返回图片url
                            }
                            return aiChatMessageVO;
        }).toList());

        return aiChatMessageVOPage;
    }
}




