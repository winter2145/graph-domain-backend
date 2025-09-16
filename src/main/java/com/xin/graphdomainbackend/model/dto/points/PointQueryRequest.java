package com.xin.graphdomainbackend.model.dto.points;

import com.xin.graphdomainbackend.model.dto.PageRequest;
import lombok.Data;

/**
 * 积分查询请求
 */
@Data
public class PointQueryRequest extends PageRequest {

    private Long userId;
}
