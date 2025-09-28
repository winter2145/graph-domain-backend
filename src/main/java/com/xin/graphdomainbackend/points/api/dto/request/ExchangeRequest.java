package com.xin.graphdomainbackend.points.api.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 用户发起兑换/升级空间的请求
 */
@Data
public class ExchangeRequest {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    @NotNull(message = "spaceId 不能为空")
    private Long spaceId;

    /**
     * 希望升级到的目标等级（0-普通 1-专业 2-旗舰）
     * */
    private Integer targetLevel;
}
