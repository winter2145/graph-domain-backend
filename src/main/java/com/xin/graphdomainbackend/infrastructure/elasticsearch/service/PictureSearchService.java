package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import com.xin.graphdomainbackend.infrastructure.elasticsearch.constant.SearchTypeConstant;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsPicture;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.util.EsQueryUtil;
import com.xin.graphdomainbackend.picture.api.dto.vo.PictureVO;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.picture.service.PictureService;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service("picture")
public class PictureSearchService extends SearchTemplate<PictureVO>{

    @Resource
    private PictureService pictureService;

    @Override
    public NativeQuery buildQuery(SearchRequest request) {
        String searchText = request.getSearchText();

        // 1. 基础 bool 条件
        EsQueryUtil.BoolQueryBuilder bool = EsQueryUtil.bool()
                .must(
                        EsQueryUtil.term("reviewStatus", 1),
                        EsQueryUtil.term("isDelete", 0)
                )
                .mustNot(EsQueryUtil.exists("spaceId"))
                .minimumShouldMatch(1);

        // 2. should 多字段匹配
        bool.should(
                EsQueryUtil.term("name.keyword", searchText, 3.0f),
                EsQueryUtil.match("name.mix", searchText, 2.5f),
                EsQueryUtil.term("category", searchText, 2.0f),
                EsQueryUtil.term("tags.keyword", searchText, 1.5f),
                EsQueryUtil.match("introduction", searchText, 1.0f)
        );

        // 3. 数字 ID 精确匹配
        try {
            long pictureId = Long.parseLong(searchText);
            bool.should(EsQueryUtil.term("id", pictureId));
        } catch (NumberFormatException ignore) {}

        // 使用父类的通用排序和分页
        return buildQueryWithSortAndPage(bool.build(), request);
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
        // 添加调试日志，输出查询结果
        log.info("查询返回结果数量: {}", hits.getTotalHits());

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