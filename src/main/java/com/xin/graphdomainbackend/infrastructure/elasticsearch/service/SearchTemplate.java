package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import com.xin.graphdomainbackend.search.service.HotSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

@Slf4j
public abstract class SearchTemplate<T> {

    // @Resource
    @Autowired(required = false)
    protected ElasticsearchOperations elasticsearchOperations;;

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

        if(elasticsearchOperations == null) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "搜索服务暂不可用"
            );
        }

        return elasticsearchOperations.search(
                query,
                getEntityClass(),
                IndexCoordinates.of(getIndexName())
        );
    }

    /**
     * 构建带有默认排序和分页的查询
     *
     * @param query   查询对象
     * @param request 搜索请求
     * @return 构建好的查询对象
     */
    protected NativeQuery buildQueryWithSortAndPage(co.elastic.clients.elasticsearch._types.query_dsl.Query query, SearchRequest request) {
        List<SortOptions> sort = List.of(
                SortOptions.of(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                SortOptions.of(so -> so.field(f -> f.field("createTime").order(SortOrder.Desc)))
        );
        PageRequest pageable = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return NativeQuery.builder()
                .withQuery(query)
                .withSort(sort)
                .withPageable(pageable)
                .build();
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