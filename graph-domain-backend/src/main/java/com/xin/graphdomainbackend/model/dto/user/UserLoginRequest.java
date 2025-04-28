package com.xin.graphdomainbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -2246745674031673666L;

    // 账号
    private String accountOrEmail;

    // 密码
    private String password;

    // 验证码
    private String verifyCode;

    //验证码ID
    private String serverIfCode;
}
