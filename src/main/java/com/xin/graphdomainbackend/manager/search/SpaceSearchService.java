package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import com.xin.graphdomainbackend.model.vo.SpaceVO;
import com.xin.graphdomainbackend.utils.ConvertObjectUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("space")
public class SpaceSearchService extends SearchTemplate<SpaceVO>{
    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("spaceName", searchText));

        try {
            Long spaceId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", spaceId));
        } catch (NumberFormatException ignored) {}

        boolQueryBuilder.minimumShouldMatch(1)
                .must(QueryBuilders.termQuery("isDelete", 0))
                // 只搜索团队空间
                .must(QueryBuilders.termQuery("spaceType", 1));

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(request.getCurrent() - 1, request.getPageSize()))
                .withSorts(
                        SortBuilders.scoreSort().order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC)
                )
                .build();
    }

    @Override
    protected Class<?> getEntityClass() {
        return EsSpace.class;
    }

    @Override
    protected String getIndexName() {
        return SearchTypeConstant.SPACE_INDEX;
    }

    @Override
    protected Page<SpaceVO> buildResult(SearchHits<?> hits, SearchRequest request) {
        List<SpaceVO> spaceVOList = hits.getSearchHits()
                .stream()
                .map(hit -> (EsSpace) hit.getContent())
                .map(ConvertObjectUtils::toSpace) // EsSpace -> Space
                .map(space -> {
                    SpaceVO spaceVO = SpaceVO.objToVo(space);

                    User user = userService.getById(space.getUserId());
                    if (user != null) {
                        spaceVO.setUser(userService.getUserVO(user));
                    }
                    return spaceVO;
                })
                .collect(Collectors.toList());

        PageRequest pageRequest = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return new PageImpl<>(
                spaceVOList,
                pageRequest,
                hits.getTotalHits()
        );
    }

}
