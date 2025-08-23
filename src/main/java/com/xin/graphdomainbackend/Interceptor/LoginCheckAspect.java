package com.xin.graphdomainbackend.Interceptor;

import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 登录检查切面
 *
 * 只验证用户是否登录，不验证具体角色权限</p>
 */
@Aspect
@Component
public class LoginCheckAspect {

    @Resource
    private UserService userService;

    /**
     * 登录检查拦截
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(loginCheck)")
    public Object checkLogin(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        // 获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 检查用户登录状态
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (BusinessException e) {
            // 捕获登录异常并重新抛出
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        } catch (Exception e) {
            // 其他异常处理
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录状态检查失败");
        }
        if (UserConstant.BAN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "此账号处于封禁中");
        }

        // 通过校验,放行
        return joinPoint.proceed();
    }
}
