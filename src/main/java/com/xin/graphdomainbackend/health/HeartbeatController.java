package com.xin.graphdomainbackend.health;

import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * 心跳检测
 */
@RestController
public class HeartbeatController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/heartbeat")
    @LoginCheck
    public void heartbeat(HttpServletRequest request) {
        // 获取当前用户
        User currentUser = userService.getLoginUser(request);
        if (currentUser != null && currentUser.getId() != null) {
            String cacheKey = "online:" + currentUser.getId();
            // 设置5分钟
            stringRedisTemplate.opsForValue().set(cacheKey, "online", Duration.ofMinutes(5));
        }
    }

}
