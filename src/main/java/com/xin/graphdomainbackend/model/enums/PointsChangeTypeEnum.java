package com.xin.graphdomainbackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 积分变化类型 枚举类
 */
@Getter
public enum PointsChangeTypeEnum {

    SIGN_IN( "签到",1),
    EXCHANGE("兑换", 2 ),
    SYSTEM("系统赠送", 3),
    OTHER("其他", 4);

    private final String text;
    private final int value;

    PointsChangeTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static PointsChangeTypeEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (PointsChangeTypeEnum pointsChangeTypeEnum : PointsChangeTypeEnum.values()) {
            if (value == pointsChangeTypeEnum.value) {
                return pointsChangeTypeEnum;
            }
        }
        return OTHER;
    }
}
