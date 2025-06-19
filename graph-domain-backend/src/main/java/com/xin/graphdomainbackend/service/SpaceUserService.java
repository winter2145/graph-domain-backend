package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.vo.UserVO;

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

}
