package com.xin.graphdomainbackend.spaceuser.api.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请加入空间请求
 */
@Data
public class SpaceUserJoinRequest implements Serializable {
    private static final long serialVersionUID = -1317852815021501754L;

    /**
     * 空间id
     */
    private Long spaceId;
}
