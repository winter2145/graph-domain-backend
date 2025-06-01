package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.DeleteRequest;
import com.xin.graphdomainbackend.model.dto.space.SpaceAddRequest;
import com.xin.graphdomainbackend.model.dto.space.SpaceQueryRequest;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-05-18 07:08:21
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequest 空间创建请求参数
     * @param loginUser 当前登录用户
     * @return 新创建的空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间参数
     *
     * @param space 空间对象
     * @param add 是否为创建校验（true-创建校验，false-更新校验）
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间 ID 删除指定空间（仅限管理员或本人）。
     *
     * @param spaceId 空间主键 ID
     * @param loginUser 当前登录用户
     */
    boolean deleteSpace(Long spaceId, User loginUser);

    /**
     * 批量删除空间
     * @param deleteRequests 删除空间请求
     */
    boolean deleteSpaceByBatch(List<DeleteRequest> deleteRequests);

    /**
     * 更新空间
     * @param space 空间对象
     */
    boolean updateSpace(Space space);

    /**
     * 获取空间包装类（单条）
     *
     * @param space 空间对象
     * @return 处理后的空间视图对象
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage 空间分页对象
     * @param request HTTP请求
     * @return 处理后的空间视图对象分页结果
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件封装
     *
     * @param spaceQueryRequest 空间查询请求参数
     * @return 查询条件封装对象
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space 空间对象
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser 当前登录用户
     * @param space 空间对象
     */
    void checkSpaceAuth(User loginUser, Space space);


}
