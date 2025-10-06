package com.xin.graphdomainbackend.infrastructure.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和AI 回复的内容
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("AI Request: {}", request.userText());
        return request;
    }

    private void after(AdvisedResponse advisedResponse) {
        try {
            if (advisedResponse != null && advisedResponse.response() != null
                    && advisedResponse.response().getResult() != null
                    && advisedResponse.response().getResult().getOutput() != null) {
                log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
            }
        } catch (Exception e) {
            log.warn("MyLoggerAdvisor error: {}", e.getMessage());
        }
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        request = before(request);
        AdvisedResponse resp = chain.nextAroundCall(request);
        after(resp);
        return resp;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        request = before(request);
        return new MessageAggregator().aggregateAdvisedResponse(chain.nextAroundStream(request), this::after);
    }
}
