package com.xin.graphdomainbackend.infrastructure.ai.client;

import com.xin.graphdomainbackend.infrastructure.ai.config.CloudflareAiClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * CF Ai Client
 */
@Component
public class CloudflareAiClient {

    private final WebClient webClient;

    private final CloudflareAiClientConfig cfClientConfig;

    public CloudflareAiClient(WebClient cloudflareWebClient,
                              CloudflareAiClientConfig cfClientConfig) {
        this.webClient = cloudflareWebClient;
        this.cfClientConfig = cfClientConfig;
    }

    /**
     * 调用 Cloudflare 图像大模型生成图片
     * @param prompt  提示词
     * @return 二进制文件
     */
    public Mono<byte[]> generateImage(String prompt) {

        String path = String.format("/accounts/%s/ai/run/%s",
                cfClientConfig.getAccountId(),
                cfClientConfig.getModel());

        return webClient.post()
                .uri(path)
                .bodyValue(Map.of("prompt", prompt))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody -> {
                            // 打印出 Cloudflare 返回的具体错误信息，方便排查 400 到底错在哪
                            return Mono.error(new RuntimeException("CF API 调用失败，错误详情: " + errorBody));
                        })
                )
                .bodyToMono(byte[].class);
    }

    /**
     * 调用 Cloudflare 文本大模型进行对话（用于 Prompt 优化）
     * @param messages 包含 role 和 content 的消息列表
     * @return AI 回复的内容文本
     */
    public Mono<String> generateText(List<Map<String, String>> messages) {
        //使用 llama-3.1-8b-instruct 模型
        String textModel = "@cf/meta/llama-3.1-8b-instruct";

        String path = String.format("/accounts/%s/ai/run/%s",
                cfClientConfig.getAccountId(),
                textModel);

        return webClient.post()
                .uri(path)
                .bodyValue(Map.of("messages", messages)) // Cloudflare 文本接口接收 messages 数组
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new RuntimeException("Cloudflare 文本生成失败: " + errorBody))
                        )
                )
                .bodyToMono(Map.class)
                .map(res -> {
                    // Cloudflare 返回的结构是: { "result": { "response": "这里是文本内容" }, "success": true ... }
                    Map<String, Object> result = (Map<String, Object>) res.get("result");
                    if (result != null && result.get("response") != null) {
                        return result.get("response").toString();
                    }
                    throw new RuntimeException("Cloudflare 未返回有效的文本结果");
                });
    }
}
