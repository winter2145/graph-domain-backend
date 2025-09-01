package com.xin.graphdomainbackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.xin.graphdomainbackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@EnableRetry
public class GraphDomainBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphDomainBackendApplication.class, args);
    }

}
