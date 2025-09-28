package com.xin.graphdomainbackend.user.api.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1269357322438266680L;

    // id
    private Long id;

    // 用户昵称
    private String userName;

    // 用户头像
    private String userAvatar;

    // 简介
    private String userProfile;

    // 用户角色：user/admin
    private String userRole;

}
