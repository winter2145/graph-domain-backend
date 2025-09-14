package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.points.ExchangeRequest;
import com.xin.graphdomainbackend.model.entity.PointsExchangeRule;
import com.xin.graphdomainbackend.model.vo.PointsExchangeRuleVO;

import java.util.List;

public interface ExchangeService extends IService<PointsExchangeRule> {

    /**
     * 兑换空间
     */
    Boolean exchangeSpace(ExchangeRequest request);

    /**
     * 获取用户的所有兑换规则，并标记是否可兑换
     */
    List<PointsExchangeRuleVO> getRulesWithStatus(Long userId);
}
