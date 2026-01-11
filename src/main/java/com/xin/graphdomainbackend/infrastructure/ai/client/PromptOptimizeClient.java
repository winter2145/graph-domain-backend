package com.xin.graphdomainbackend.infrastructure.ai.client;

import com.xin.graphdomainbackend.infrastructure.websocket.chat.dao.entity.ChatMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface PromptOptimizeClient {
    /**
     * 提示词优化接口
     * @param systemPrompt 系统设定
     * @param history 历史消息上下文
     * @param userPrompt 当前用户输入
     * @return 优化后的英文提示词
     */
    String optimize(String systemPrompt,
                    List<Message> history,
                    String userPrompt);
}
