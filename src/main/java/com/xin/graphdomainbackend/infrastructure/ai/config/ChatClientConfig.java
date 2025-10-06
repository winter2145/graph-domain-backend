package com.xin.graphdomainbackend.infrastructure.ai.config;

import com.xin.graphdomainbackend.infrastructure.ai.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory) {   // 会自动注入你自己写的 MybatisChatMemory
        return builder
                .defaultAdvisors(
                        // 1. 先让内存顾问把历史带进去（只读）
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 2. 再打日志（也只读）
                        new MyLoggerAdvisor()
                )
                .build();
    }
}
