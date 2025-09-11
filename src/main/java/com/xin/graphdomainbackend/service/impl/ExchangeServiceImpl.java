package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.PointsExchangeRuleMapper;
import com.xin.graphdomainbackend.mapper.SpaceMapper;
import com.xin.graphdomainbackend.mapper.UserPointsAccountMapper;
import com.xin.graphdomainbackend.model.dto.points.ExchangeRequest;
import com.xin.graphdomainbackend.model.entity.PointsExchangeRule;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.UserPointsAccount;
import com.xin.graphdomainbackend.model.enums.SpaceLevelEnum;
import com.xin.graphdomainbackend.model.vo.PointsExchangeRuleVO;
import com.xin.graphdomainbackend.service.ExchangeService;
import com.xin.graphdomainbackend.service.PointsService;
import com.xin.graphdomainbackend.service.SpaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExchangeServiceImpl extends ServiceImpl<PointsExchangeRuleMapper, PointsExchangeRule>
        implements ExchangeService {

    @Resource
    private PointsService pointsService;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserPointsAccountMapper userPointsAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean exchangeSpace(ExchangeRequest request) {
        Long userId = request.getUserId();
        Long spaceId = request.getSpaceId();

        // 查找空间
        Space space = spaceMapper.selectById(spaceId);
        if (space == null || !space.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不存在或无权限");
        }

        // 查规则
        PointsExchangeRule rule = this.lambdaQuery()
                .eq(PointsExchangeRule::getFromLevel, space.getSpaceLevel())
                .eq(PointsExchangeRule::getToLevel, request.getTargetLevel())
                .one();

        if (rule == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有对应的升级规则");
        }

        // 扣积分
        pointsService.deductPoints(userId, rule.getCostPoints(), "兑换空间升级");

        // 升级空间
        return spaceService.upgradeSpaceBySpaceLevel(rule.getToLevel(), space, userId);
    }

    @Override
    public List<PointsExchangeRuleVO> getRulesWithStatus(Long userId) {
        // 查用户账户积分
        UserPointsAccount account = userPointsAccountMapper.selectOne(
                new QueryWrapper<UserPointsAccount>().eq("userId", userId));
        int availablePoints = (account != null) ? account.getAvailablePoints() : 0;

        // 查用户当前最低的空间等级
        Space space = spaceMapper.selectOne(
                new QueryWrapper<Space>()
                        .eq("userId", userId)
                        .orderByAsc("spaceLevel")   // 级别最小的排第一
                        .last("LIMIT 1"));
        int userLevel = (space != null) ? space.getSpaceLevel() : SpaceLevelEnum.COMMON.getValue();

        // 查所有规则
        List<PointsExchangeRule> rules = this.list(
                new QueryWrapper<PointsExchangeRule>().orderByAsc("fromLevel"));

        // 转换为 VO，并计算 canExchange
        return rules.stream().map(rule -> {
            PointsExchangeRuleVO vo = new PointsExchangeRuleVO();
            vo.setId(rule.getId());
            Integer toLevel = rule.getToLevel();
            vo.setToLevel(toLevel);

            SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(toLevel);
            if (spaceLevelEnum != null) {
                String toLevelName = spaceLevelEnum.getText();
                vo.setToLevelName(toLevelName);
            }

            vo.setCostPoints(rule.getCostPoints());

            boolean can = (rule.getFromLevel() == userLevel) && (availablePoints >= rule.getCostPoints());
            vo.setCanExchange(can);
            return vo;
        }).collect(Collectors.toList());
    }

}
