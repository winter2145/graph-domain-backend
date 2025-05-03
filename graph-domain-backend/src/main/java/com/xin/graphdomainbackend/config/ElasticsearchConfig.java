package com.xin.graphdomainbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Elasticsearch 配置类
 * 提供ES客户端配置、连接池管理、健康检查和日期转换器
 */
@Slf4j
@Configuration
@EnableScheduling
public class ElasticsearchConfig {

    // 从配置文件中读取 Elasticsearch 地址，例如：http://localhost:9200
    @Value("${spring.elasticsearch.uris}")  // 没有默认值
    private String uris;

    // 可选：仅在生产环境中使用
    @Value("${spring.elasticsearch.username:}") // 冒号后面为默认值,null
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.profiles.active:dev}") // 默认值为dev
    private String activeProfile;

    // 支持的日期格式定义
    private static final String[] DATE_FORMATS = new String[] {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    // 连接池配置
    private static final int MAX_CONN_TOTAL = 100;
    private static final int MAX_CONN_PER_ROUTE = 100;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 300000;
    private static final int KEEP_ALIVE_TIME = 300000;

    private RestHighLevelClient client;

    /**
     * 创建并配置 Elasticsearch 客户端
     */
    @Bean(destroyMethod = "close")
    public RestHighLevelClient elasticsearchClient() {
        // 解析URI并创建HttpHost
        try {
            String cleanUri = uris.replace("https://", "").replace("http://", "");
            String[] hostParts = cleanUri.split(":");
            String hostname = hostParts[0];
            int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 9200;

            RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, "http"));

            // 配置默认请求头
            builder.setDefaultHeaders(new org.apache.http.Header[]{
                    new org.apache.http.message.BasicHeader("Content-Type", "application/json"),
                    new org.apache.http.message.BasicHeader("Accept", "application/json")
            });

            // 配置HTTP客户端
            configureHttpClient(builder);

            // 配置请求超时
            builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .setConnectionRequestTimeout(0));

            // 配置失败监听器
            builder.setFailureListener(new RestClient.FailureListener() {
                @Override
                public void onFailure(Node node) {
                    log.error("Node {} failed", node.getName());
                }
            });

            client = new RestHighLevelClient(builder);
            return client;
        } catch (Exception e) {
            log.error("Failed to create Elasticsearch client :{}", e.getMessage());
            throw new RuntimeException("Could not create Elasticsearch client", e);
        }
    }

    /**
     * 配置HTTP客户端
     */
    private void configureHttpClient(RestClientBuilder builder) {
        if ("prod".equals(activeProfile) && username != null && password != null) {
            // 生产环境配置认证
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setMaxConnTotal(MAX_CONN_TOTAL)
                    .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                    .setKeepAliveStrategy((response, context) -> KEEP_ALIVE_TIME)
                    .addInterceptorLast((HttpResponseInterceptor)(response, context) -> {
                        if (response.getStatusLine().getStatusCode() >= 500) {
                            log.warn("Received server error from Elasticsearch");
                        }
                    }));
        } else {
            // 开发环境配置
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setMaxConnTotal(MAX_CONN_TOTAL)
                    .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                    .setKeepAliveStrategy((response, context) -> KEEP_ALIVE_TIME));
        }
    }

    /**
     * 定时检查ES连接状态
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void checkout() {
        if (client != null) {
            try {
                boolean isConnected = client.ping(RequestOptions.DEFAULT);
                if (!isConnected) {
                    log.warn("Lost connection to Elasticsearch, attempting to reconnect...");
                    // 重连
                    reconnect();
                }
            } catch (Exception e) {
                log.error("Error checking Elasticsearch connection: {}", e.getMessage());
            }
        } else {
            log.warn("Elasticsearch client is null, attempting to initialize...");
            client = elasticsearchClient();
        }
    }

    /**
     * 重连机制
     */
    private void reconnect() {
        int maxRetries = 3;
        int retryCount = 0;
        boolean connected = false;

        while (!connected && retryCount < maxRetries) {
            log.info("Attempting reconnection, try {}/{}", retryCount + 1, maxRetries);
            try {
                // 使用递增的等待时间
                TimeUnit.SECONDS.sleep(5 * (retryCount + 1));

                // 安全关闭客户端
                closeClientSafely();

                // 重新配置客户端
                client = elasticsearchClient();

                if (client.ping(RequestOptions.DEFAULT)) {
                    connected = true;
                    log.info("Successfully reconnected to Elasticsearch");
                }


            } catch (Exception e) {
                retryCount++;
                log.error("Retry {} failed: {}", retryCount, e.getMessage());
            }
        }
    }

    /**
     * 安全关闭客户端
     */
    private void closeClientSafely() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("Error closing old client", e);
            }
        }
    }

    /**
     * 配置ES日期转换器
     *//*
    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
                Arrays.asList(new DateToStringConverter(), new StringToDateConverter())
        );
    }

    *//**
     * 日期转字符串转换器
     *//*
    @WritingConverter
    static class DateToStringConverter implements org.springframework.core.convert.converter.Converter<Date, String> {
        @Override
        public String convert(Date source) {
            if (source == null) {
                return null;
            }
            return String.format("%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tLZ", source);
        }
    }

    *//**
     * 字符串转日期转换器
     *//*
    @ReadingConverter
    static class StringToDateConverter implements org.springframework.core.convert.converter.Converter<String, Date> {
        @Override
        public Date convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            try {
                return DateUtils.parseDate(source, DATE_FORMATS);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Failed to parse date: " + source, e);
            }
        }
    }*/
}