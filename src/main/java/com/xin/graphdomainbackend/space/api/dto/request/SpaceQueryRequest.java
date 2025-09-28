package com.xin.graphdomainbackend.space.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 8739840121877290109L;

    // id
    private Long id;

    // 用户id
    private Long userId;

    // 空间名称
    private String spaceName;

    // 空间级别 0-普通  1-专业  2-旗舰
    private Integer spaceLevel;

    // 空间类型 0-私有  1-团队
    private Integer spaceType;

}
