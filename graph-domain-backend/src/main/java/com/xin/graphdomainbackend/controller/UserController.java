package com.xin.graphdomainbackend.controller;

import cn.hutool.core.util.StrUtil;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.user.EmailCodeRequest;
import com.xin.graphdomainbackend.model.dto.user.UserRegisterRequest;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 获取邮箱验证码
     */
    @PostMapping("/get_emailcode")
    public BaseResponse<Boolean> getEmailCode(@RequestBody EmailCodeRequest emailCodeRequest, HttpServletRequest request) {
        if (emailCodeRequest == null || StrUtil.hasBlank(emailCodeRequest.getEmail(),emailCodeRequest.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = emailCodeRequest.getEmail();
        String type = emailCodeRequest.getType();
        boolean result = userService.sendEmailCode(email, type, request);

        return ResultUtils.success(result);
    }
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        ThrowUtils.throwIf(registerRequest == null, ErrorCode.PARAMS_ERROR);
        String email = registerRequest.getEmail();
        String userPassword = registerRequest.getUserPassword();
        String code = registerRequest.getCode();
        String checkPassword = registerRequest.getCheckPassword();
        long result = userService.userRegister(email, userPassword, checkPassword, code);

        return ResultUtils.success(result);
    }
}
