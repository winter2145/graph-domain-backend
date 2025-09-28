package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsSpace;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.util.EsQueryUtil;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import com.xin.graphdomainbackend.space.api.dto.vo.SpaceVO;
import com.xin.graphdomainbackend.space.dao.entity.Space;
import com.xin.graphdomainbackend.space.service.SpaceService;
import com.xin.graphdomainbackend.spaceuser.service.SpaceUserService;
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

@Service("space")
public class SpaceSearchService extends SearchTemplate<SpaceVO>{

    @Resource
    private SpaceService spaceService;

    @Resource
    protected UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

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
                EsQueryUtil.term("spaceName.keyword", searchText, 3.0f),
                EsQueryUtil.match("spaceName.ik", searchText, 2.0f)
        );

        // 3. 数字 ID 精确匹配
        try {
            long spaceId = Long.parseLong(searchText);
            bool.should(EsQueryUtil.term("id", spaceId));
        } catch (NumberFormatException ignore) {}

        // 使用父类的通用排序和分页
        return buildQueryWithSortAndPage(bool.build(), request);
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
            // 批量获取成员数量
            Map<Long, Integer> spaceMemberCount = spaceUserService.getSpaceMemberCount(spaceIds);

            // 按照原始Id顺序重现构建结果
            spaceVOList = spaceIds.stream()
                    .map(spaceMap::get)
                    .filter(Objects::nonNull)
                    .map(space -> {
                        SpaceVO spaceVO = SpaceVO.objToVo(space);
                        Long spaceId = space.getId();
                        spaceVO.setMemberCount(spaceMemberCount.get(spaceId));

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