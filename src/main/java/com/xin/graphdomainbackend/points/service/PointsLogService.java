package com.xin.graphdomainbackend.points.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsLogVO;
import com.xin.graphdomainbackend.points.dao.entity.UserPointsLog;


public interface PointsLogService extends IService<UserPointsLog> {

    /**
     * 分页获取用户积分流水
     */
    Page<PointsLogVO> getUserPointsLogs(Long userId, int pageNum, int pageSize);

    /**
     * 插入用户的积分流水
     */
    Boolean addPointLog(UserPointsLog userPointsLog);
}
