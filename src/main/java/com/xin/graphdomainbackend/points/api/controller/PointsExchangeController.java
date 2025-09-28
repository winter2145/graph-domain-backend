package com.xin.graphdomainbackend.points.api.controller;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.points.api.dto.request.ExchangeRequest;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsExchangeRuleVO;
import com.xin.graphdomainbackend.points.service.ExchangeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exchange")
public class PointsExchangeController {

    @Resource
    private ExchangeService exchangeService;

    // 兑换空间
    @PostMapping("/space")
    @LoginCheck
    public BaseResponse<Boolean> exchangeSpace(@RequestBody ExchangeRequest request) {
        return ResultUtils.success(exchangeService.exchangeSpace(request));
    }

    // 获取用户的所有兑换规则，并标记是否可兑换
    @GetMapping("/rules")
    @LoginCheck
    public BaseResponse<List<PointsExchangeRuleVO>> getRules(@RequestParam Long userId) {
        return ResultUtils.success(exchangeService.getRulesWithStatus(userId));
    }
}