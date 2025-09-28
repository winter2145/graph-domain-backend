package com.xin.graphdomainbackend.search.api.controller;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.search.api.dto.request.SearchRequest;
import com.xin.graphdomainbackend.search.service.HotSearchService;
import com.xin.graphdomainbackend.search.service.SearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {
    @Resource
    private SearchService searchService;

    @Resource
    private HotSearchService hotSearchService;

    @PostMapping("/all")
    public BaseResponse<Page<?>> searchAll(@RequestBody SearchRequest searchRequest) {
        return ResultUtils.success(searchService.doSearch(searchRequest));
    }

    @GetMapping("/hot")
    public BaseResponse<List<String>> hotSearchTerms(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "8") Integer size) {

        // 参数校验
        if (!StringUtils.hasText(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-8之间");
        }
        // 校验搜索类型
        if (!type.matches("^(picture|user|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        return ResultUtils.success(hotSearchService.getHotSearchList(type, size));
    }
}
