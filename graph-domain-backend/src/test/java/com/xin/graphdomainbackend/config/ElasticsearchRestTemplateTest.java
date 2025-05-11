package com.xin.graphdomainbackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/*
@SpringBootTest
public class ElasticsearchRestTemplateTest {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void testConnection() {
        // 尝试获取ES集群信息，判断能否连通
        boolean connected = elasticsearchRestTemplate.indexOps(org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of("test")).exists();
        assertThat(connected).isNotNull(); // 只要不抛异常就说明能连上
        System.out.println("Elasticsearch 连接成功！");
    }
}
*/
