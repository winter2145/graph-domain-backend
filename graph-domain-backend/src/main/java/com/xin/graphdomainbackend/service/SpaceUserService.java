package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.space.SpaceQueryRequest;
import com.xin.graphdomainbackend.model.dto.spaceuser.*;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.SpaceUserVO;
import com.xin.graphdomainbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-06-15 10:20:45
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 获取空间内的所有成员 (脱敏)
     *
     * @param spaceId 空间id
     */
    List<UserVO> getAllSpaceMembers(Long spaceId);

    /**
     * 判断成员是否属于该空间
     *
     * @param userId 用户id
     * @param spaceId 空间id
     */
    boolean isSpaceMember(long userId, long spaceId);

    /**
     * 空间内添加成员
     *
     * @param spaceUserAddRequest 添加成员请求
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员对象
     *
     * @param spaceUser 空间成员
     * @param add true为创建，false为编辑
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 编辑空间成员角色权限
     * @param spaceUserEditRequest 成员编辑请求
     */
    boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

    /**
     * 构建查询对象 请求
     * @param spaceUserQueryRequest 空间_用户 查询请求
     * @return
     */
    LambdaQueryWrapper<SpaceUser> getLamQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员 包装类（单条）
     * @param spaceUser 用户空间对象

     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员 包装类（列表）
     * @param spaceUserList 用户空间列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 审核空间成员申请
     *
     * @param spaceUserAuditRequest 申请请求
     * @param loginUser 当前登录用户
     */
    boolean auditSpaceUser(SpaceUserAuditRequest spaceUserAuditRequest, User loginUser);

    /**
     * 申请加入空间
     * @param spaceUserJoinRequest 空间加入请求
     * @param loginUser 当前登录用户
     */
    boolean joinSpace(SpaceUserJoinRequest spaceUserJoinRequest, User loginUser);

}
