package com.xin.graphdomainbackend.aop;

import com.xin.graphdomainbackend.annotation.AuthCheck;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.UserRoleEnum;
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

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 权限校验拦截器（基于注解 @AuthCheck）
     * 使用 Spring AOP 的 @Around 注解实现环绕通知
     */
    @Around("@annotation(authCheck)") // 拦截所有带有 @AuthCheck 注解的方法
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取注解参数
        String mustRole = authCheck.mustRole();

        // 获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();

        // 转换为HttpServletRequest对象
        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();

        // 解析目标权限情况
        UserRoleEnum targetUserEnum = UserRoleEnum.getUser(mustRole);

        // 如果注解未指定权限要求（mustRole 为空），直接放行
        if (targetUserEnum == null) {
            return joinPoint.proceed();// 执行原方法
        }

        // 检查用户登录状态
        User loginUser = userService.getLoginUser(request);

        UserRoleEnum  loginUserEnum= UserRoleEnum.getUser(loginUser.getUserRole());

        if (loginUser == null || targetUserEnum != loginUserEnum) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过校验,放行
        return joinPoint.proceed();

    }
}
