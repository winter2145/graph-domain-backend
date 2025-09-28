package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsUser;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.util.EsQueryUtil;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("user")
public class UserSearchService extends SearchTemplate<UserVO> {
    @Resource
    private UserService userService;

    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        // 1. 基础 bool 条件
        EsQueryUtil.BoolQueryBuilder bool = EsQueryUtil.bool()
                .must(
                        EsQueryUtil.term("isDelete", 0)
                )
                .minimumShouldMatch(1);

        // 2. should 多字段匹配
        bool.should(
                EsQueryUtil.term("userName.keyword", searchText, 3.0f),
                EsQueryUtil.match("userName.mix", searchText, 2.5f),
                EsQueryUtil.match("userAccount", searchText, 2.0f),
                EsQueryUtil.match("userProfile", searchText, 1.0f)
        );

        // 3. 数字 ID 精确匹配
        try {
            long userId = Long.parseLong(searchText);
            bool.should(EsQueryUtil.term("id", userId, 5.0f));
        } catch (NumberFormatException ignore) {}

        // 使用父类的通用排序和分页
        return buildQueryWithSortAndPage(bool.build(), request);
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