package com.xin.graphdomainbackend.userfollows.api.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowersAndFansVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 粉丝数量
     */
    private Long fansCount;
    /**
     * 关注数量
     */
    private Long followCount;
}
