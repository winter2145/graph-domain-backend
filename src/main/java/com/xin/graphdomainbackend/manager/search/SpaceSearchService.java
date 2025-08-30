package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import com.xin.graphdomainbackend.model.vo.SpaceVO;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ConvertObjectUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("space")
public class SpaceSearchService extends SearchTemplate<SpaceVO>{

    @Resource
    private SpaceService spaceService;

    @Resource
    protected UserService userService;

    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("isDelete", 0))
                // 只搜索团队空间
                .must(QueryBuilders.termQuery("spaceType", 1));

        // 构建应该匹配的条件（支持搜索的字段）
        BoolQueryBuilder shouldQueries = QueryBuilders.boolQuery();

        // 文本字段搜索（带权重）
        shouldQueries.should(QueryBuilders.termQuery("spaceName", searchText)
                .boost(3.0f));
        shouldQueries.should(QueryBuilders.matchQuery("spaceName.ik", searchText)
                .boost(2.0f));

        // 精确匹配用户ID（如果搜索文本是数字）
        try {
            Long spaceId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", spaceId));
        } catch (NumberFormatException ignored) {}


        // 将should查询添加到主查询, 至少命中一个 should 条件
        boolQueryBuilder.should(shouldQueries);
        boolQueryBuilder.minimumShouldMatch(1);

        // 构建排序策略
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC));  // 相关度评分优先
        sorts.add(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));  // 时间排序最后

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(request.getCurrent() - 1, request.getPageSize()))
                .withSorts(sorts)
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

        // 从ES查出匹配的 EsPicture 文档
        List<Long> spaceIds = hits.getSearchHits().stream()
                .map(hit -> (EsSpace) hit.getContent())
                .map(EsSpace::getId)
                .collect(Collectors.toList());

        List<SpaceVO> spaceVOList;
        if (spaceIds.isEmpty()) {
            spaceVOList = Collections.emptyList();
        } else { // 根据空间Id获取完整的Space对象
            List<Space> spaces = spaceService.listByIds(spaceIds);

            // 创建Id到实体的映射，便于快速查找
            Map<Long, Space> spaceMap = spaces.stream()
                    .collect(Collectors.toMap(Space::getId, Function.identity()));

            // 按照原始Id顺序重现构建结果
            spaceVOList = spaceIds.stream()
                    .map(spaceMap::get)
                    .filter(Objects::nonNull)
                    .map(space -> {
                        SpaceVO spaceVO = SpaceVO.objToVo(space);

                        User user = userService.getById(space.getUserId());
                        if (user != null) {
                            spaceVO.setUser(userService.getUserVO(user));
                        }
                        return spaceVO;
                    })
                    .collect(Collectors.toList());
        }

        PageRequest pageRequest = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return new PageImpl<>(
                spaceVOList,
                pageRequest,
                hits.getTotalHits()
        );
    }

}
