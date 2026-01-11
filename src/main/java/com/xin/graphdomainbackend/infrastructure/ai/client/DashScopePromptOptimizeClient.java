package com.xin.graphdomainbackend.infrastructure.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashScopePromptOptimizeClient implements PromptOptimizeClient {

    private final ChatClient chatClient;

    public DashScopePromptOptimizeClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String optimize(String systemPrompt,
                           List<Message> history,
                           String userPrompt) {

        return chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(userPrompt)
                .call()
                .content();
    }
}
