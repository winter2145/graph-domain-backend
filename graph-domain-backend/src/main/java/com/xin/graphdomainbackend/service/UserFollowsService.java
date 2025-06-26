package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowersQueryRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsAddRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.xin.graphdomainbackend.model.entity.UserFollows;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.vo.FollowersAndFansVO;
import com.xin.graphdomainbackend.model.vo.UserVO;

import java.util.List;

/**
* @author Administrator
* @description 针对表【user_follows】的数据库操作Service
* @createDate 2025-06-21 21:45:59
*/
public interface UserFollowsService extends IService<UserFollows> {

    /**
     * 关注按钮的操作
     * @param userFollowsAddRequest 关注他人请求
     */
    Boolean addUserFollows(UserFollowsAddRequest userFollowsAddRequest);

    /**
     * 查询是否关注
     * @param userFollowsIsFollowsRequest 关注查询请求
     */
    Boolean findIsFollow(UserFollowsIsFollowsRequest userFollowsIsFollowsRequest);

    /**
     *  查询关注、粉丝列表
     * @param userFollowersQueryRequest 关注查询请求
     */
    Page<UserVO> getFollowOrFanList(UserFollowersQueryRequest userFollowersQueryRequest);

    /**
     * 查找本人的关注与粉丝数量
     * @param id 用户id
     */
    FollowersAndFansVO getFollowAndFansCount(Long id);

    /**
     * 根据用户id，查询所有关注的列表中的id(去空)
     * @param id 目标用户id
     */
    List<Long> getFollowList(Long id);
}
