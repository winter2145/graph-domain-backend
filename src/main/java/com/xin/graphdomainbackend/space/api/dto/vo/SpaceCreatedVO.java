package com.xin.graphdomainbackend.space.api.dto.vo;

import lombok.Data;

/**
 * 用户创建的空间 视图
 */
@Data
public class SpaceCreatedVO {
    // id
    private Long id;

    // 空间名称
    private String spaceName;

    // 空间级别  0-普通版 1-专业版 2-旗舰版
    private Integer spaceLevel;

    // 空间级别名称  0-普通版 1-专业版 2-旗舰版
    private String spaceLevelName;

    // 空间类型 0-私有 1-团队
    private  Integer spaceType;;

    // 空间类型名称  0-私人 1-团队
    private String spaceTypeName;

    // 创建用户ID
    private Long useId;

    // 是否可兑换
    private Boolean canExchange;

}
