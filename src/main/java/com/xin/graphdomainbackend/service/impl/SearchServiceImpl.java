package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.esdao.EsSearchKeyDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.search.SearchTemplate;
import com.xin.graphdomainbackend.mapper.HotSearchMapper;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.HotSearch;
import com.xin.graphdomainbackend.model.entity.es.EsSearchKeyword;
import com.xin.graphdomainbackend.service.HotSearchService;
import com.xin.graphdomainbackend.service.SearchService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
