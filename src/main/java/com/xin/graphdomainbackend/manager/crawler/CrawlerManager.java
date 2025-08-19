package com.xin.graphdomainbackend.manager.crawler;

import com.xin.graphdomainbackend.constant.CrawlerConstant;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 爬虫管理类
 */
@Component
@Slf4j
public class CrawlerManager {

    @Resource
    private UserService userService;


    private void detectRequest(HttpServletRequest request, int warnCount, int banCount) {
        User loginUser = null;
        String key;
        String identifier;

        Boolean isLogin = userService.isLogin(request);
        if (isLogin) { 
            loginUser = userService.getLoginUser(request);
            identifier = String.valueOf(loginUser.getId());
            key = String.format("%s:%s",CrawlerConstant.USER_KEY, identifier);
        } else {
            identifier = getClientIpAddress(request);
            key = String.format("%s:%s",CrawlerConstant.IP_KEY, identifier);
        }

    }

    /**
     * 获取客户端IP
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
