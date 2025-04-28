package com.xin.graphdomainbackend.model.dto.user;

import com.xin.graphdomainbackend.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 7779942694352135160L;

    // id
    private Long id;

    // 用户昵称
    private String userName;

    // 账号
    private String userAccount;

    // 简介
    private String userProfile;

    // 用户角色：user/admin/ban
    private String userRole;

}
