package com.xin.graphdomainbackend.controller;

import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.model.dto.points.ExchangeRequest;
import com.xin.graphdomainbackend.model.vo.PointsExchangeRuleVO;
import com.xin.graphdomainbackend.service.ExchangeService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/exchange")
public class ExchangeController {

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