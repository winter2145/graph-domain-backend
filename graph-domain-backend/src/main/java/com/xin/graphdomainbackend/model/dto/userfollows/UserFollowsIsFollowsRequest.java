package com.xin.graphdomainbackend.model.dto.userfollows;

import lombok.Data;

/**
 * 是否关注请求
 */
@Data
public class UserFollowsIsFollowsRequest {

    /**
     * 关注者的用户 ID
     */
    private Long followerId;

    /**
     * 被关注者的用户 ID
     */
    private Long followingId;
}
