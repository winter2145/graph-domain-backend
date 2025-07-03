package com.xin.graphdomainbackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间成员请求
 */
@Data
public class SpaceUserQueryRequest implements Serializable {

    private static final long serialVersionUID = -5544859932667299956L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
