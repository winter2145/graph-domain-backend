package com.xin.graphdomainbackend.user.api.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件发送请求
 */
@Data
public class EmailCodeRequest implements Serializable {

    private static final long serialVersionUID = -4935896962776534759L;

    // 邮件类型
    // register-注册，resetPassword-重置密码，changeEmail-修改邮箱
    private String type;

    // 邮箱
    private String email;

}
