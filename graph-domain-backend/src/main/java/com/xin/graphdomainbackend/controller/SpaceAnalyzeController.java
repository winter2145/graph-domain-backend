package com.xin.graphdomainbackend.controller;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xin.graphdomainbackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.xin.graphdomainbackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xin.graphdomainbackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xin.graphdomainbackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.xin.graphdomainbackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xin.graphdomainbackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.xin.graphdomainbackend.service.SpaceAnalyzeService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间分析
 */
@Slf4j
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    UserService userService;

    /**
     * 分析空间使用情况（图片数量、大小)
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);

        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 获取空间图片分类分析
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest categoryAnalyzeRequest
            ,HttpServletRequest request) {
        ThrowUtils.throwIf(categoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> responseList = spaceAnalyzeService.getSpaceCategoryAnalyze(categoryAnalyzeRequest, loginUser);

        return ResultUtils.success(responseList);
    }

    /**
     * 获取空间图片标签分析
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest
            ,HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> responseList = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);

        return ResultUtils.success(responseList);
    }

    /**
     * 分段统计图片大小
     */
    @PostMapping("size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
            @RequestBody SpaceSizeAnalyzeRequest sizeAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(sizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> responseList = spaceAnalyzeService.getSpaceSizeAnalyze(sizeAnalyzeRequest, loginUser);

        return ResultUtils.success(responseList);
    }
}
