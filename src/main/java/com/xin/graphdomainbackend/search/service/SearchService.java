package com.xin.graphdomainbackend.search.service;

import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import org.springframework.data.domain.Page;

public interface SearchService {

    /**
     * 统一搜索接口
     * @param searchRequest 搜索请求
     */
    Page<?> doSearch(SearchRequest searchRequest);

}
