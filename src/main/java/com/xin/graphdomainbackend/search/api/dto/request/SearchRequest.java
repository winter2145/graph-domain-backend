package com.xin.graphdomainbackend.search.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import lombok.Data;

/**
 * 搜索请求类
 */
@Data
public class SearchRequest extends PageRequest {

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 搜索类型
     */
    private String type;

}
