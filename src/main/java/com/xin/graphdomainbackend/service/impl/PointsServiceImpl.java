package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.UserPointsAccountMapper;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.UserPointsAccount;
import com.xin.graphdomainbackend.model.entity.UserPointsLog;
import com.xin.graphdomainbackend.model.enums.PointsChangeTypeEnum;
import com.xin.graphdomainbackend.model.vo.PointsInfoVO;
import com.xin.graphdomainbackend.service.PointsLogService;
import com.xin.graphdomainbackend.service.PointsService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PointsServiceImpl extends ServiceImpl<UserPointsAccountMapper, UserPointsAccount>
        implements PointsService {

    @Resource
    private PointsLogService pointsLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, int points, String remark) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        UserPointsAccount existAccount = this.baseMapper.selectByUserId(userId);
        UserPointsAccount newAccount = new UserPointsAccount();

        int currentPoints;
        int totalPoints;
        newAccount.setUserId(userId);
        if (existAccount == null) { // 插入用户积分
            currentPoints = points;
            totalPoints = points;
            newAccount.setTotalPoints(currentPoints);
            newAccount.setAvailablePoints(totalPoints);
            this.baseMapper.insert(newAccount);
        } else { // 更新用户积分
            currentPoints = existAccount.getAvailablePoints() + points;
            totalPoints = existAccount.getTotalPoints() + points;
            this.lambdaUpdate()
                    .eq(UserPointsAccount::getUserId, userId)
                    .set(UserPointsAccount::getAvailablePoints, currentPoints)
                    .set(UserPointsAccount::getTotalPoints, totalPoints)
                    .update();
        }

        // 插入用户积分流水
        UserPointsLog userPointsLog = new UserPointsLog();
        userPointsLog.setUserId(userId);
        userPointsLog.setBeforePoints(currentPoints - points);
        userPointsLog.setAfterPoints(currentPoints);
        userPointsLog.setRemark(remark); // 添加备注
        userPointsLog.setChangePoints(points); //积分变化值
        userPointsLog.setChangeType(PointsChangeTypeEnum.SIGN_IN.getValue()); // 用户签到

        pointsLogService.addPointLog(userPointsLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, int points, String remark) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        UserPointsAccount account = this.baseMapper.selectByUserId(userId);
        if (account == null || account.getAvailablePoints() < points) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足");
        }

        Integer afterPoints = account.getAvailablePoints() - points;
        account.setAvailablePoints(afterPoints);
        this.baseMapper.updateById(account);

        // 插入用户积分流水
        UserPointsLog userPointsLog = new UserPointsLog();
        userPointsLog.setUserId(userId);
        userPointsLog.setBeforePoints(afterPoints + points);
        userPointsLog.setAfterPoints(afterPoints);
        userPointsLog.setRemark(remark); // 添加备注
        userPointsLog.setChangePoints(-points); //积分变化值
        userPointsLog.setChangeType(PointsChangeTypeEnum.EXCHANGE.getValue()); // 用户兑换

        pointsLogService.addPointLog(userPointsLog);
    }

    @Override
    public UserPointsAccount getPointsInfo(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);

        return this.baseMapper.selectByUserId(userId);
    }

    @Override
    public PointsInfoVO getPointsInfoVO(UserPointsAccount account) {
        if (account == null) {
            return null;
        }

        PointsInfoVO pointsInfoVO = new PointsInfoVO();
        BeanUtils.copyProperties(account, pointsInfoVO);
        return pointsInfoVO;
    }

    @Override
    public List<PointsInfoVO> getPointsInfoVOList(List<UserPointsAccount> userPointsAccountList) {
        if (CollectionUtils.isEmpty(userPointsAccountList)) {
            return Collections.emptyList();
        }

        return userPointsAccountList.stream()
                .filter(Objects::nonNull)
                .map(this::getPointsInfoVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PointsInfoVO> getPointsInfoVOByPage(PageRequest pageRequest) {
        // 校验参数
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pageRequest.getCurrent();
        long pageSize = pageRequest.getPageSize();

        // 创建与数据库查询到的userPointsAccount一样大的分页
        Page<UserPointsAccount> pointsAccountPage = this.page(new Page<>(current, pageSize));
        Page<PointsInfoVO> pointsInfoVOPage = new Page<>(current, pageSize, pointsAccountPage.getTotal());
        List<UserPointsAccount> pointsAccounts = pointsAccountPage.getRecords();

        //获取脱敏后的数据
        List<PointsInfoVO> pointsInfoVOList = this.getPointsInfoVOList(pointsAccounts);

        // 将脱敏后的数据存放到空白分页中
        pointsInfoVOPage.setRecords(pointsInfoVOList);

        return pointsInfoVOPage;
    }

}
