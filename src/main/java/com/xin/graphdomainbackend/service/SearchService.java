package com.xin.graphdomainbackend.service;

import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public interface SearchService {

    /**
     * 统一搜索接口
     * @param searchRequest 搜索请求
     */
    Page<?> doSearch(SearchRequest searchRequest);

}
