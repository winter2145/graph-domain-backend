package com.xin.graphdomainbackend.model.enums;

import lombok.Getter;

/**
 * 评论来源枚举
 * 来源 本人（我发的）
 * 来源 他人（我收到的）
 */
@Getter
public enum MessageSourceEnum {
    FROM_ME("我发的", "FROM_ME"),
    TO_ME("我收到的", "TO_ME");

    private final String text;
    private final String value;

    MessageSourceEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MessageSourceEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        for (MessageSourceEnum sourceEnum : MessageSourceEnum.values()) {
            if (sourceEnum.value.equals(value)) {
                return sourceEnum;
            }
        }
        return null;
    }
}
