package com.xin.graphdomainbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.xin.graphdomainbackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class GraphDomainBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphDomainBackendApplication.class, args);
    }

}
