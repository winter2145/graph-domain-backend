package com.xin.graphdomainbackend.picture.api.dto.requeset;

import lombok.Data;

import java.util.List;

/**
 * 管理员批量操作图片
 */
@Data
public class PictureOperationRequest {

    /**
     * 图片id列表
     */
    private List<Long> ids;

    /**
     * 操作类型
     */
    private Integer operationType;
}
