package com.xin.graphdomainbackend.Interceptor;

import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class OnlineStatusInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果用户已经退出（Session 无登录态），不再续期 Redis
        if (request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE) == null) {
            return true;
        }

        // 获取当前登录用户
        User currentUser = userService.getLoginUser(request);
        if (currentUser != null && currentUser.getId() != null) {
            String cacheKey = "online:" + currentUser.getId();
            Boolean hasKey = stringRedisTemplate.hasKey(cacheKey);
            if (Boolean.FALSE.equals(hasKey)) {
                // 用户之前离线，现在上线，设置5分钟过期
                stringRedisTemplate.opsForValue().set(cacheKey, "online", Duration.ofMinutes(5));
            } else {
                // 续期在线状态
                stringRedisTemplate.expire(cacheKey, 5, TimeUnit.MINUTES);
            }
        }
        return true; // 放行
    }
}