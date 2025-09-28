package com.xin.graphdomainbackend.space.api.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.DeleteRequest;
import com.xin.graphdomainbackend.common.aop.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.infrastructure.auth.manager.SpaceUserAuthManager;
import com.xin.graphdomainbackend.space.api.dto.request.SpaceAddRequest;
import com.xin.graphdomainbackend.space.api.dto.request.SpaceQueryRequest;
import com.xin.graphdomainbackend.space.api.dto.request.SpaceUpdateRequest;
import com.xin.graphdomainbackend.space.api.dto.vo.SpaceCreatedVO;
import com.xin.graphdomainbackend.space.api.dto.vo.SpaceLevelVO;
import com.xin.graphdomainbackend.space.api.dto.vo.SpaceVO;
import com.xin.graphdomainbackend.space.dao.entity.Space;
import com.xin.graphdomainbackend.space.enums.SpaceLevelEnum;
import com.xin.graphdomainbackend.space.service.SpaceService;
import com.xin.graphdomainbackend.user.constant.UserConstant;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);

        return ResultUtils.success(newId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long spaceId = deleteRequest.getId();
        Space targetSpace = spaceService.getById(spaceId);
        Long userId = targetSpace.getUserId();

        // 仅本人或管理员才能删除空间
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isMySelf = userId.equals(loginUser.getId());

        if (!isAdmin && !isMySelf) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户，无删除他人图片的权限");
        }

        boolean result = spaceService.deleteSpace(spaceId, loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 批量删除空间（仅管理员可用）
     */
    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchSpace(@RequestBody List<DeleteRequest> deleteRequests) {
        if (CollectionUtils.isEmpty(deleteRequests)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = spaceService.deleteSpaceByBatch(deleteRequests);

        return ResultUtils.success(result);
    }

    /**
     * 更新空间（仅管理员可用）
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, space);
        boolean result = spaceService.updateSpace(space);

        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam("id") long id) {
        if (ObjUtil.isEmpty(id) || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（主要面向普通用户）
     */
    @GetMapping("/get/vo")
    @LoginCheck
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam("id") long id,
                                                HttpServletRequest request) {
        if (ObjUtil.isEmpty(id) || request == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space);

        // 为VO 设置 权限值
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);

        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（主要面向普通用户）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest)
        );
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage, request);

        return ResultUtils.success(spaceVOPage);
    }

    /*
    * 获取空间级别列表，便于前端展示
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevelVO>> listSpaceLevel() {
        List<SpaceLevelVO> spaceLevelVOList = Arrays
                .stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevelVO(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());

        return ResultUtils.success(spaceLevelVOList);
    }

    /**
     * 获取用户下的所有空间（我的空间 + 团队空间）
     */
    @PostMapping("/list/page/created")
    @LoginCheck
    public BaseResponse<Page<SpaceCreatedVO>> listCreatedSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                                       HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        Long userId = spaceQueryRequest.getUserId();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();

        SpaceQueryRequest queryRequest = new SpaceQueryRequest();
        queryRequest.setUserId(userId);


        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size),
                spaceService.getQueryWrapper(queryRequest)
        );

        Page<SpaceCreatedVO> spaceVOPage = spaceService.getCreatedSpaceVOByPage(spacePage, spaceLevel);

        return ResultUtils.success(spaceVOPage);
    }

}
