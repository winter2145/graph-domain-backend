package com.xin.graphdomainbackend.infrastructure.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@Profile("es")
@EnableScheduling
@EnableElasticsearchRepositories(
        basePackages = "com.xin.graphdomainbackend.infrastructure.elasticsearch.dao"
)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /* ---------- 连接池参数 ---------- */
    private static final int MAX_CONN_TOTAL = 100;
    private static final int MAX_CONN_PER_ROUTE = 100;
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int SOCKET_TIMEOUT  = 300_000;
    private static final long KEEP_ALIVE_TIME = 300_000;

    private ElasticsearchClient client;
    private RestClient restClient;

    /* ---------- 主 Bean：ElasticsearchClient ---------- */
    @Bean
    public ElasticsearchClient elasticsearchClient() {

            /* 1. 解析 URI */
            String cleanUri = uris.replace("https://", "").replace("http://", "");
            String[] parts  = cleanUri.split(":");
            String hostname = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;

            /* 2. 构建 RestClient */
            RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, "http"))
                    .setRequestConfigCallback(cfg -> cfg
                            .setConnectTimeout(CONNECT_TIMEOUT)
                            .setSocketTimeout(SOCKET_TIMEOUT))
                    .setHttpClientConfigCallback(this::httpClientConfig);

            restClient = builder.build();

            /* 3. 构建 ElasticsearchClient */
            ElasticsearchTransport transport =
                    new RestClientTransport(restClient, new JacksonJsonpMapper());
            client = new ElasticsearchClient(transport);
            return client;
    }
    @Bean
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchClient client) {
        return new ElasticsearchTemplate(client);
    }

    /* ---------- 连接池 / 认证 ---------- */
    private HttpAsyncClientBuilder httpClientConfig(HttpAsyncClientBuilder b) {
        if ("prod".equals(activeProfile) && username != null && password != null) {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            b.setDefaultCredentialsProvider(provider);
        }
        return b.setMaxConnTotal(MAX_CONN_TOTAL)
                .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                .setKeepAliveStrategy((r, c) -> KEEP_ALIVE_TIME);
    }

    /* ---------- 定时探活 ---------- */
    @Scheduled(fixedRate = 30_000)
    public void checkOut() {
        if (client == null) {
            return;
        }

        try {
            boolean ok = client.ping().value();
            if (!ok) {
                log.warn("ES 连接丢失，尝试重连 ...");
                reconnect();
            }
        } catch (Exception e) {
            log.error("探活失败", e);
            reconnect();
        }
    }

    /* ---------- 重连机制 ---------- */
    private void reconnect() {
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                TimeUnit.SECONDS.sleep(5L * i);
                closeSafely();
                elasticsearchClient();          // 重新初始化
                if (client.ping().value()) {
                    log.info("重连成功");
                    return;
                }
            } catch (Exception ex) {
                log.error("第 {} 次重连失败", i, ex);
            }
        }
    }

    /* ---------- 关闭 ---------- */
    @PreDestroy
    public void destroy() {
        // Spring 容器关闭时会自动调用此方法
        log.info("关闭 Elasticsearch 连接");
        closeSafely();
    }

    /* ---------- 关闭 ---------- */
    private void closeSafely() {
        if (restClient != null) {
            try {
                restClient.close();
            } catch (IOException e) {
                log.warn("关闭 restClient 异常", e);
            }
        }
    }
}