package com.xin.graphdomainbackend.userfollows.api.dto.request;

import com.xin.graphdomainbackend.common.PageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


import java.io.Serializable;

/**
 * 关注查询请求
 */
@Data
public class UserFollowersQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 6015898495989771142L;
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
     * 搜索类型,0为关注，1为粉丝
     */
    @NotNull(message = "searchType不能为空")
    private Integer searchType;

}
