package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.mapper.UserPointsLogMapper;
import com.xin.graphdomainbackend.model.entity.UserPointsLog;
import com.xin.graphdomainbackend.model.vo.PointsLogVO;
import com.xin.graphdomainbackend.service.PointsLogService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class PointsLogServiceImpl extends ServiceImpl<UserPointsLogMapper, UserPointsLog>
        implements PointsLogService {

    @Override
    public Page<PointsLogVO> getUserPointsLogs(Long userId, int pageNum, int pageSize) {
        Page<UserPointsLog> page = new Page<>(pageNum, pageSize);

        Page<UserPointsLog> entityPage = this.lambdaQuery()
                .eq(UserPointsLog::getUserId, userId)
                .orderByDesc(UserPointsLog::getCreateTime)
                .page(page);

        // 转换为 VO
        Page<PointsLogVO> voPage = new Page<>(pageNum, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(UserPointsLog::objToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public Boolean addPointLog(UserPointsLog userPointsLog) {
        return this.save(userPointsLog);
    }
}