package com.xin.graphdomainbackend.category.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;

/**
 * 分类查询请求
 */
@Data
public class CategoryQueryRequest extends PageRequest {

    /**
     * 分类名称
     */
    private String categoryName;

}
