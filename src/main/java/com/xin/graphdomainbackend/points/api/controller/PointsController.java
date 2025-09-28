package com.xin.graphdomainbackend.points.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.points.api.dto.request.PointQueryRequest;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsInfoVO;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsLogVO;
import com.xin.graphdomainbackend.points.dao.entity.UserPointsAccount;
import com.xin.graphdomainbackend.points.service.PointsLogService;
import com.xin.graphdomainbackend.points.service.PointsService;
import com.xin.graphdomainbackend.user.constant.UserConstant;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/points")
public class PointsController {

    @Resource
    private PointsLogService pointsLogService;

    @Resource
    private PointsService pointsService;

    // 分页查询积分流水
    @GetMapping("/logs")
    @LoginCheck
    public BaseResponse<Page<PointsLogVO>> getUserPointsLogs(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "5") int pageSize) {
        return ResultUtils.success(pointsLogService.getUserPointsLogs(userId, pageNum, pageSize));
    }

    // 查询积分
    @GetMapping("/info")
    @LoginCheck
    public BaseResponse<PointsInfoVO> getPointsInfo(@RequestParam Long userId) {
        UserPointsAccount pointsInfo = pointsService.getPointsInfo(userId);
        return ResultUtils.success(pointsService.getPointsInfoVO(pointsInfo));
    }

    //分页查询所有人的积分列表（管理员）
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PointsInfoVO>> getPointsInfoList(PointQueryRequest pointQueryRequest) {
        Page<PointsInfoVO> pointsInfoVOByPage = pointsService.getPointsInfoVOByPage(pointQueryRequest);

        return ResultUtils.success(pointsInfoVOByPage);
    }

}