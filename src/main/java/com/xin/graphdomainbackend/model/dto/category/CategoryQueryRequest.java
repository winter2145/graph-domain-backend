package com.xin.graphdomainbackend.model.dto.category;

import com.xin.graphdomainbackend.model.dto.PageRequest;
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
