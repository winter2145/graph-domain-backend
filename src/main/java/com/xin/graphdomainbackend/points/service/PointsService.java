package com.xin.graphdomainbackend.points.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.points.api.dto.request.PointQueryRequest;
import com.xin.graphdomainbackend.points.api.dto.vo.PointsInfoVO;
import com.xin.graphdomainbackend.points.dao.entity.UserPointsAccount;


import java.util.List;

public interface PointsService extends IService<UserPointsAccount> {

    /**
     * 用户积分增加
     * @param userId 用户id
     * @param points 积分数量
     * @param remark 备注
     */
    void addPoints(Long userId, int points, String remark);

    /**
     * 用户积分减少
     */
    void deductPoints(Long userId, int points, String remark);

    /**
     * 获取个人积分
     * @param userId 用户id
     */
    UserPointsAccount getPointsInfo(Long userId);

    /**
     * 获取个人积分VO（个人）
     * @param userPointsAccount 用户积分账户
     */
    PointsInfoVO getPointsInfoVO(UserPointsAccount userPointsAccount);

    /**
     * 获取所有人的积分列表 （管理员）
     */
    List<PointsInfoVO> getPointsInfoVOList(List<UserPointsAccount> userPointsAccountList);

    /**
     * 分页获取所有人积分（管理员）
     */
    Page<PointsInfoVO> getPointsInfoVOByPage(PointQueryRequest pointQueryRequest);
}
