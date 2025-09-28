package com.xin.graphdomainbackend.spaceanalyze.api.controller;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.space.dao.entity.Space;
import com.xin.graphdomainbackend.spaceanalyze.api.dto.request.*;
import com.xin.graphdomainbackend.spaceanalyze.api.dto.vo.*;
import com.xin.graphdomainbackend.spaceanalyze.service.SpaceAnalyzeService;
import com.xin.graphdomainbackend.user.constant.UserConstant;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 用户行为分析
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
            @RequestBody SpaceUserAnalyzeRequest userAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(userAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        List<SpaceUserAnalyzeResponse> responseList = spaceAnalyzeService.getSpaceUserAnalyze(userAnalyzeRequest, loginUser);

        return ResultUtils.success(responseList);
    }

    /**
     * 空间使用排行分析（仅管理员可用）
     */
    @PostMapping("/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<Space> resultList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }
}
