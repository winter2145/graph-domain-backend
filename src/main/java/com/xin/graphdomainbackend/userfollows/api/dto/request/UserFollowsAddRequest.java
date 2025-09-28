package com.xin.graphdomainbackend.userfollows.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 用户关注请求
 */
@Data
public class UserFollowsAddRequest {

    /**
     * 关注者的用户 ID
     */
    @NotNull(message = "followerId不能为空")
    private Long followerId;

    /**
     * 被关注者的用户 ID
     */
    @NotNull(message = "followingId不能为空")
    private Long followingId;

    /**
     * 关注状态，0 表示取消关注，1 表示关注
     */
    @NotNull(message = "followStatus不能为空")
    private Integer followStatus;

}
