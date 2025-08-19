package com.xin.graphdomainbackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加空间成员请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色： viewer、editor、admin
     */
    private String spaceRole;
}
