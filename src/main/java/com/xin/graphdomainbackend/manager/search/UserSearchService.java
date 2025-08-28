package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsUser;
import com.xin.graphdomainbackend.model.vo.UserVO;
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

@Component("user")
public class UserSearchService extends SearchTemplate<UserVO> {
    @Resource
    private UserService userService;

    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        // 主布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("isDelete", 0)); // 必须满足的条件

        // 构建应该匹配的条件（支持搜索的字段）
        BoolQueryBuilder shouldQueries = QueryBuilders.boolQuery();

        // 文本字段搜索（带权重）
        shouldQueries.should(QueryBuilders.termQuery("userName.keyword", searchText)
                .boost(3.0f)); // 用户名权重最高
        shouldQueries.should(QueryBuilders.matchQuery("userName.mix", searchText)
                .boost(2.5f));
        shouldQueries.should(QueryBuilders.matchQuery("userAccount", searchText)
                .boost(2.0f));
        shouldQueries.should(QueryBuilders.matchQuery("userProfile", searchText)
                .boost(1.0f));;

        // 精确匹配用户ID（如果搜索文本是数字）
        try {
            Long userId = Long.parseLong(searchText);
            shouldQueries.should(QueryBuilders.termQuery("id", userId).boost(5.0f));
        } catch (NumberFormatException ignored) {}

        // 将should查询添加到主查询, 至少命中一个 should 条件
        boolQueryBuilder.should(shouldQueries);
        boolQueryBuilder.minimumShouldMatch(1);

        // 构建排序策略
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC)); // 相关度评分优先
        sorts.add(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)); // 时间排序最后

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(request.getCurrent() - 1, request.getPageSize()))
                .withSorts(sorts)
                .build();

    }

    @Override
    protected Class<?> getEntityClass() {
        return EsUser.class;
    }

    @Override
    protected String getIndexName() {
        return SearchTypeConstant.USER_INDEX;
    }

    @Override
    protected Page<UserVO> buildResult(SearchHits<?> hits, SearchRequest request) {

        // 从ES查出匹配的 EsUser 文档
        List<Long> userIds = hits.getSearchHits().stream()
                .map(hit ->(EsUser) hit.getContent())
                .map(EsUser::getId)
                .collect(Collectors.toList());

        List<UserVO> userVOList;
        if (userIds.isEmpty()) {
            userVOList = Collections.emptyList();
        } else { // 再根据ID从DB获取完整User对象（保证数据是最新的）
            List<User> users = userService.listByIds(userIds);

            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));

            userVOList = userIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(userService::getUserVO)
                    .collect(Collectors.toList());
        }

        PageRequest pageRequest = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return new PageImpl<>(
                userVOList,
                pageRequest,
                hits.getTotalHits()
        );
    }
}
