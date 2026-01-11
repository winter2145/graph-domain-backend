package com.xin.graphdomainbackend.infrastructure.ai.client;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class CloudflarePromptOptimizeClient implements PromptOptimizeClient {

    @Resource
    private CloudflareAiClient cloudflareAiClient;

    @Override
    public String optimize(String systemPrompt, List<Message> history, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. 系统提示词
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 2. 遍历历史记录 (Spring AI Message -> Cloudflare Map)
        if (history != null) {
            for (Message msg : history) {
                // getMessageType().getValue() 会返回 "user", "assistant", "system" 等
                messages.add(Map.of(
                        "role", msg.getMessageType().getValue(),
                        "content", msg.getText()
                ));
            }
        }

        // 3. 当前用户输入
        messages.add(Map.of("role", "user", "content", userPrompt));

        // 4. 调用底层 WebClient 获取优化结果
        return cloudflareAiClient.generateText(messages).block();
    }
}