package com.xin.graphdomainbackend.model.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SignInVO {

    // 用户id
    private Long userId;

    // 本次签到获得的积分,通常为1
    private Integer gainedPoints;

    // 签到的日期
    private LocalDate signDate;
}
