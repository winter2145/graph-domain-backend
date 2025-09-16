package com.xin.graphdomainbackend.manager.crawler;


import com.xin.graphdomainbackend.config.MailSendConfig;
import com.xin.graphdomainbackend.constant.CrawlerConstant;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 爬虫管理类
 */
@Component
@Slf4j
public class CrawlerManager {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CounterManagerService counterManagerService;

    @Resource
    private MailSendConfig emailSenderUtil;


    /**
     * 通用检测请求
     * @param request http请求
     * @param warnCount 警告阙值
     * @param banCount 禁用阙值
     */
    private void detectRequest(HttpServletRequest request, int warnCount, int banCount) {
        User loginUser = null;
        String key;
        String identifier;

        Boolean isLogin = userService.isLogin(request);
        if (isLogin) { // 已登录, 获取用户Id
            loginUser = userService.getLoginUser(request);
            identifier = String.valueOf(loginUser.getId());
            key = String.format("%s:%s",CrawlerConstant.USER_KEY, identifier);
        } else { // 未登录, 获取Ip
            identifier = getClientIpAddress(request);
            key = String.format("%s:%s",CrawlerConstant.IP_KEY, identifier);
        }

        // 统计2分钟内的访问次数(150秒后过期)
        long count = counterManagerService.incrAndGetCounter(key, 2, TimeUnit.MINUTES, CrawlerConstant.EXPIRE_TIME);

        if (count > banCount) { // 禁用处理
            if (loginUser != null) {
                // 管理员只记录警告日志，不进行封禁
                if (userService.isAdmin(loginUser)) {
                    log.warn("警告：管理员访问频繁, userId={}, count={}", loginUser.getId(), count);
                    return;
                }
                // 对于普通用户，封号并踢下线
                User updateUser = new User();
                updateUser.setId(loginUser.getId());
                updateUser.setUserRole(UserConstant.BAN_ROLE); // 设置禁用
                userService.updateById(updateUser);

                // 发送爬虫警告邮件
                try {
                    sendCrawlerWarningEmail(loginUser, identifier, count);
                } catch (Exception e) {
                    log.error("发送爬虫警告邮件失败", e);
                }

                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问次数过多，已被封号");
            } else {
                // 对于未登录用户，封禁ip，24h
                banIp(identifier);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问次数过多，IP 已被封禁");
            }
        } else if (count > warnCount) { // 警告处理
            // 记录警告日志
            if (loginUser != null) {
                log.warn("警告：用户访问频繁, userId={}, count={}", loginUser.getId(), count);
            } else {
                log.warn("警告：IP访问频繁, ip={}, count={}", identifier, count);
            }
        }
    }

    /**
     * 检测普通请求（警告 - 30，禁止 - 50）
     * @param request http请求
     */
    public void detectNormalRequest(HttpServletRequest request) {
        detectRequest(request, CrawlerConstant.WARN_COUNT, CrawlerConstant.BAN_COUNT);
    }

    /**
     * 检测高频操作请求（警告 - 15，禁止 - 25）
     */
    public void detectFrequentRequest(HttpServletRequest request) {
        detectRequest(request, CrawlerConstant.WARN_COUNT / 2, CrawlerConstant.BAN_COUNT / 2);
    }


    /**
     * 发送爬虫警告邮件
     */
    private void sendCrawlerWarningEmail(User user, String ipAddress, long accessCount) throws IOException {
        ClassPathResource resource = new ClassPathResource("html/crawler_warning.html");
        String template;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            template = content.toString();
        }

        // 替换模板中的变量
        SimpleDateFormat sdf = new SimpleDateFormat("yyuyy-MM-dd HH:mm:ss");
        String emailContent = template
                .replace(":userId", String.valueOf(user.getId()))
                .replace(":userAccount", user.getUserAccount())
                .replace(":userEmail", user.getEmail())
                .replace(":ipAddress", ipAddress)
                .replace(":accessCount", String.valueOf(accessCount))
                .replace(":detectTime", sdf.format(new Date()));

        // 发送邮件
        emailSenderUtil.sendReviewEmail(emailSenderUtil.getAdmin(), emailContent);
    }

    /**
     * 封禁IP, 24h
     */
    private void banIp(String ip) {
        String banKey = String.format("%s:%s", CrawlerConstant.BAN_IP_KEY, ip);
        stringRedisTemplate.opsForValue().set(banKey, "1", 24, TimeUnit.HOURS);
    }

    /**
     * 检查ip是否被封
     * 封 - > true
     */
    private boolean isIpBanned(String ip) {
        String banKey = String.format("%s:%s", CrawlerConstant.BAN_IP_KEY, ip);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(banKey));
    }

    /**
     * 解封Ip
     */
    private void unblock(String ip) {
        String banKey = String.format("%s:%s", CrawlerConstant.BAN_IP_KEY, ip);
        if (isIpBanned(ip)) {
            stringRedisTemplate.delete(banKey);
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
