package com.xin.graphdomainbackend.points.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.points.api.dto.request.ExchangeRequest;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsExchangeRuleVO;
import com.xin.graphdomainbackend.points.dao.entity.PointsExchangeRule;

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
