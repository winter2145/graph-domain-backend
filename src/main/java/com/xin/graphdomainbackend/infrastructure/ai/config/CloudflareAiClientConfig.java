package com.xin.graphdomainbackend.infrastructure.ai.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * CloudflareAi 配置类
 */
@Configuration
@ConfigurationProperties(prefix = "cloudflare.ai")
@Data
public class CloudflareAiClientConfig {

    private String apiToken;

    private String accountId;

    private String model;

    @Bean
    public WebClient cloudflareWebClient() {

        // 配置 5MB 的缓冲区限制 (5 * 1024 * 1024)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl("https://api.cloudflare.com/client/v4")
                .exchangeStrategies(strategies) // 应用缓冲区策略
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + this.apiToken
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }
}
