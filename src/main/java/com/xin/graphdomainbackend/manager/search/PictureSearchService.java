package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import com.xin.graphdomainbackend.model.vo.PictureVO;
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

@Component("picture")
public class PictureSearchService extends SearchTemplate<PictureVO>{
    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("name", searchText))
                .should(QueryBuilders.matchQuery("introduction", searchText))
                .should(QueryBuilders.matchQuery("tags", searchText));

        try {
            Long pictureId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", pictureId));
        } catch (NumberFormatException ignored) {
            // 输入不为数字，忽略错误
        }

        boolQueryBuilder.minimumShouldMatch(1) // 至少满足1个条件
                .must(QueryBuilders.termQuery("reviewStatus", 1)) // 已审核
                .mustNot(QueryBuilders.existsQuery("spaceId")) // 公共空间
                .must(QueryBuilders.termQuery("isDelete", 0)); // 必须满足的条件

        int current = request.getCurrent() - 1;
        int pageSize = request.getPageSize();

        // 使用org.springframework.data.domain包
        PageRequest pageRequest = PageRequest.of(current, pageSize);

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(
                        SortBuilders.scoreSort().order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC)
                )
                .build();
    }

    @Override
    protected Class<?> getEntityClass() {
        return EsPicture.class;
    }

    @Override
    protected String getIndexName() {
        return  SearchTypeConstant.PICTURE_INDEX;
    }

    @Override
    protected Page<PictureVO> buildResult(SearchHits<?> hits, SearchRequest request) {
        List<PictureVO> pictureVOList = hits.getSearchHits()
                .stream()
                .map(hit -> (EsPicture) hit.getContent())
                .map(ConvertObjectUtils::toPicture) // EsPicture -> Picture
                .map(picture -> {
                    PictureVO pictureVO = PictureVO.objToVo(picture); // 脱敏图片信息

                    User user = userService.getById(picture.getUserId()); // 脱敏用户信息
                    if (user != null) {
                        pictureVO.setUser(userService.getUserVO(user));
                    }

                    return pictureVO;
                })
                .collect(Collectors.toList());

        PageRequest pageRequest = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return new PageImpl<>(
                pictureVOList,           // 当前页的数据列表
                pageRequest,             // 分页信息
                hits.getTotalHits()      // 总记录数
        );
    }

}
