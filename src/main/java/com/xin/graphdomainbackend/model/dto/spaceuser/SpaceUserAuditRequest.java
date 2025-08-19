package com.xin.graphdomainbackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核空间成员请求
 */
@Data
public class SpaceUserAuditRequest implements Serializable {

    private static final long serialVersionUID = 4071181819237614801L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 被审核用户ID
     */
    private Long userId;

    /**
     * 审核结果：1-通过 2-拒绝
     */
    private Integer status;

}
