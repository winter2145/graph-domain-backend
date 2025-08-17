package com.xin.graphdomainbackend.config;

import com.xin.graphdomainbackend.Interceptor.OnlineStatusInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private OnlineStatusInterceptor onlineStatusInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有需要登录的接口（可根据实际情况调整路径）
        registry.addInterceptor(onlineStatusInterceptor)
                .addPathPatterns("/**") // 全局拦截
                .excludePathPatterns(
                    "/user/login",
                    "/user/register",
                    "/user/getCode",
                    "/user/get_emailcode",
                    "/heartbeat",
                    "/picture/list/page/vo",
                    "/picture/tag_category"
                    // 其他不需要登录的接口
                );
    }
}