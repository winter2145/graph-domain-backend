package com.xin.graphdomainbackend.search.service.impl;

import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.service.SearchTemplate;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import com.xin.graphdomainbackend.search.service.SearchService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Resource
    private Map<String, SearchTemplate<?>> stringSearchTemplateMap;

    @Override
    public Page<?> doSearch(SearchRequest searchRequest) {
        // 校验参数
        ThrowUtils.throwIf(searchRequest == null, ErrorCode.PARAMS_ERROR);

        String searchText = searchRequest.getSearchText();
        String type = searchRequest.getType();

        ThrowUtils.throwIf(searchText.isBlank(), ErrorCode.PARAMS_ERROR, "搜索文本不能为空");
        // 直接从 map 中根据类型获取对应的搜索策略
        SearchTemplate<?> searchStrategy = stringSearchTemplateMap.get(type);

        if (searchStrategy != null) {
            return searchStrategy.search(searchRequest);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }
    }
}
