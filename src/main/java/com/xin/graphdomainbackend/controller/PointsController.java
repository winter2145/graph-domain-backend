package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.AuthCheck;
import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.dto.points.PointQueryRequest;
import com.xin.graphdomainbackend.model.entity.UserPointsAccount;
import com.xin.graphdomainbackend.model.vo.PointsInfoVO;
import com.xin.graphdomainbackend.model.vo.PointsLogVO;
import com.xin.graphdomainbackend.service.PointsLogService;
import com.xin.graphdomainbackend.service.PointsService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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