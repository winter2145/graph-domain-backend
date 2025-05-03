package com.xin.graphdomainbackend.config;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ElasticsearchConfigTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    public void testElasticsearchConnection() throws Exception {
        boolean connected = restHighLevelClient.ping(RequestOptions.DEFAULT);
        assertThat(connected).isTrue();
        System.out.println("Elasticsearch 配置类连接成功！");
    }
}
