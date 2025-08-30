package com.xin.graphdomainbackend.manager.search;

import com.xin.graphdomainbackend.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.model.dto.search.SearchRequest;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.service.PictureService;
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

@Component("picture")
public class PictureSearchService extends SearchTemplate<PictureVO>{

    @Resource
    private PictureService pictureService;

    @Override
    protected Query buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        // 主布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery() // 必须满足的条件
                .must(QueryBuilders.termQuery("reviewStatus", 1))  // 已审核
                .mustNot(QueryBuilders.existsQuery("spaceId"))  // 公共空间
                .must(QueryBuilders.termQuery("isDelete", 0));  // 未删除


        // 构建应该匹配的条件(支持搜索的字段)
        BoolQueryBuilder shouldQueries = QueryBuilders.boolQuery();

        // 文本字段搜索（带权重）
        shouldQueries.should(QueryBuilders.termQuery("name.keyword", searchText)
                .boost(3.0f)); // 用户名权重最高
        shouldQueries.should(QueryBuilders.matchQuery("name.mix", searchText)
                .boost(2.5f));
        shouldQueries.should(QueryBuilders.termQuery("category", searchText)
                .boost(2.0f));
        shouldQueries.should(QueryBuilders.termQuery("tags.keyword", searchText)
                .boost(1.5f));
        shouldQueries.should(QueryBuilders.matchQuery("introduction", searchText)
                .boost(1.0f));

        // 精确匹配用户ID（如果搜索文本是数字）
        try {
            Long pictureId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", pictureId));
        } catch (NumberFormatException ignored) {
            // 输入不为数字，忽略错误
        }

        // 将should查询添加到主查询, 至少命中一个 should 条件
        boolQueryBuilder.should(shouldQueries);
        boolQueryBuilder.minimumShouldMatch(1);

        // 构建排序策略
        List<SortBuilder<?>> sorts = new ArrayList<>();
        sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC));  // 相关度评分优先
        sorts.add(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));  // 时间排序最后

        int current = request.getCurrent() - 1;
        int pageSize = request.getPageSize();

        // 使用org.springframework.data.domain包
        PageRequest pageRequest = PageRequest.of(current, pageSize);

        return new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sorts)
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

        // 从ES查出匹配的 EsPicture 文档
        List<Long> pictureIds = hits.getSearchHits().stream()
                .map(hit -> (EsPicture) hit.getContent())
                .map(EsPicture::getId)
                .collect(Collectors.toList());

        List<PictureVO> pictureVOList;
        if (pictureIds.isEmpty()) {
            pictureVOList = Collections.emptyList();
        } else { // 根据图片id获取完整的Picture对象（保证数据是最新的）
            List<Picture> pictures = pictureService.listByIds(pictureIds);

            // 创建ID到实体的映射，便于快速查找
            Map<Long, Picture> pictureMap = pictures.stream()
                    .collect(Collectors.toMap(Picture::getId, Function.identity()));

            // 按照原始Id顺序重现构建结果
            List<Picture> orderedPictures  = pictureIds.stream()
                    .map(pictureMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            pictureVOList = pictureService.getPictureVOList(orderedPictures);
        }

        PageRequest pageRequest = PageRequest.of(request.getCurrent() - 1, request.getPageSize());

        return new PageImpl<>(
                pictureVOList,           // 当前页的数据列表
                pageRequest,             // 分页信息
                hits.getTotalHits()      // 总记录数
        );
    }

}
