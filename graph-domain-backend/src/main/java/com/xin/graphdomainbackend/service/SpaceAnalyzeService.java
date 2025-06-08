package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.space.analyze.*;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {
    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest 空间分析请求
     * @param loginUser 登录用户
     */
    void checkAnalyzeSpaceAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 补充分析查询参数
     * @param spaceAnalyzeRequest 空间分析请求
     * @param queryWrapper 查询请求
     */
    void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper);

    /**
     * 获取空间使用分析数据
     *
     * @param usageAnalyzeRequest  请求参数
     * @param loginUser                当前登录用户
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest usageAnalyzeRequest
            , User loginUser);

    /**
     * 获取空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest
            , User loginUser);


    /**
     * 获取空间图片标签分析
     *
     * @param spaceTagAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片大小分析
     *
     * @param sizeAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest sizeAnalyzeRequest, User loginUser);


    /**
     * 获取空间用户上传行为分析
     *
     * @param userAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest userAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行分析（仅管理员）
     *
     * @param spaceRankAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

}
