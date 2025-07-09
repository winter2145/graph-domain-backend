package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.DeleteRequest;
import com.xin.graphdomainbackend.model.dto.spaceuser.*;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.SpaceUserVO;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    SpaceUserService spaceUserService;

    @Resource
    UserService userService;

    /**
     * 管理员添加成员到空间
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 管理员移除成员
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || request == null, ErrorCode.PARAMS_ERROR);

        long id = deleteRequest.getId();
        // 判断是是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean result = spaceUserService.removeById(id);

        return ResultUtils.success(result);
    }

    /**
     * 编辑成员权限
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = spaceUserService.editSpaceUser(spaceUserEditRequest);

        return ResultUtils.success(result);
    }

    /**
     * 查询成员列表信息
     */
    @PostMapping("list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUserVO(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<SpaceUser> lamQueryWrapper = spaceUserService.getLamQueryWrapper(spaceUserQueryRequest);
        List<SpaceUser> list = spaceUserService.list(lamQueryWrapper);

        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);

        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getLamQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace() {
        // 获取请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(requestAttributes == null, ErrorCode.PARAMS_ERROR, "未获取到请求信息");
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();

        // 查询
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUserId);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getLamQueryWrapper(spaceUserQueryRequest)
        );

        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 审核空间成员申请
     */
    @PostMapping("/audit")
    public BaseResponse<Boolean> auditSpaceUser(@RequestBody SpaceUserAuditRequest spaceUserAuditRequest) {
        ThrowUtils.throwIf(spaceUserAuditRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(requestAttributes == null, ErrorCode.PARAMS_ERROR, "未获取到请求信息");
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        boolean result = spaceUserService.auditSpaceUser(spaceUserAuditRequest, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * 成员申请加入请求
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinSpace(@RequestBody SpaceUserJoinRequest spaceUserJoinRequest) {
        ThrowUtils.throwIf(spaceUserJoinRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(requestAttributes == null, ErrorCode.PARAMS_ERROR, "未获取到请求信息");
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        boolean result = spaceUserService.joinSpace(spaceUserJoinRequest, loginUser);

        return ResultUtils.success(result);

    }

}
