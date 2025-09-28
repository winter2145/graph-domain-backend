package com.xin.graphdomainbackend.space.api.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 */
@Data
public class SpaceAddRequest implements Serializable {

    private static final long serialVersionUID = 7784283863475229605L;

    // 空间名称
    private String spaceName;

    // 空间类型
    private Integer spaceType;

    // 空间级别
    private Integer spaceLevel;
}
