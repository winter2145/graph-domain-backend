package com.xin.graphdomainbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 2477684928039790384L;

    // 邮箱
    private String email;

    // 验证码
    private String code;

    // 密码
    private String userPassword;

    // 确认密码
    private String checkPassword;
}
