package com.xin.graphdomainbackend.points.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;

/**
 * 积分查询请求
 */
@Data
public class PointQueryRequest extends PageRequest {

    private Long userId;
}
