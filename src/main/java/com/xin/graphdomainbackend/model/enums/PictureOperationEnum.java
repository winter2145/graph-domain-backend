package com.xin.graphdomainbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片操作枚举类
 */
@Getter
public enum PictureOperationEnum {

    DELETE("删除", 0),
    APPROVE("通过", 1),
    REJECT("拒绝", 2);

    private final String text;

    private final int value;

    PictureOperationEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static PictureOperationEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }

        for (PictureOperationEnum operationEnum : PictureOperationEnum.values()) {
            if (operationEnum.value == value) {
                return operationEnum;
            }
        }
        return null;
    }
}
