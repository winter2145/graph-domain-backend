package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.service.HotSearchService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import javax.annotation.Resource;

public abstract class SearchTemplate<T> {

    @Resource
    protected ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    protected HotSearchService hotSearchService;

    public Page<T> search(SearchRequest request) {

        // 校验参数
        validate(request);

        // 记录搜索关键字
        recordSearchKeyword(request.getSearchText(), request.getType());

        // 构建查询条件
        Query query = buildQuery(request);

        // 执行查询
        SearchHits<?> hits = executeSearch(query);

        // 返回结果
        return buildResult(hits, request);
    }

    /**
     * 校验搜索请求参数
     */
    protected void validate(SearchRequest searchRequest) {
        ThrowUtils.throwIf(searchRequest == null, ErrorCode.PARAMS_ERROR);

        if (searchRequest.getSearchText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }
    }

    /**
     * 执行搜索查询 - 通用方法
     * 使用elasticsearchRestTemplate执行查询，子类可直接使用
     *
     * @param query 构建好的查询对象
     * @return 搜索结果
     */
    protected SearchHits<?> executeSearch(Query query) {
        return elasticsearchRestTemplate.search(query, getEntityClass(), IndexCoordinates.of(getIndexName()));
    }

    /**
     * 构建搜索查询条件 - 抽象方法
     */
    protected abstract Query buildQuery(SearchRequest request);

    /**
     * 获取实体类类型 - 抽象方法
     */
    protected abstract Class<?> getEntityClass();

    /**
     * 获取Elasticsearch索引名称 - 抽象方法
     */
    protected abstract String getIndexName();

    /**
     * 封装搜索结果
     * @param hits  Elasticsearch返回的搜索结果
     * @param request 搜索请求参数
     * @return 封装好的分页结果
     */
    protected abstract Page<T> buildResult(SearchHits<?> hits, SearchRequest request);

    /**
     * 记录搜索关键词（可选操作）
     * 热门搜索统计等功能
     */
    protected void recordSearchKeyword(String searchText, String type) {
        hotSearchService.recordSearchKeyword(searchText, type);
    }
}
