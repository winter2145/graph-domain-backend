package com.xin.graphdomainbackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据字符串值获取对应的用户角色枚举对象
     * @param value 角色值，例如 "user" 或 "admin"
     * @return 对应的UserRoleEnum，如果未匹配到则返回 null
     */
    public static UserRoleEnum getUser(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRole : UserRoleEnum.values()) {
            if (userRole.getValue().equals(value)) {
                return userRole;
            }
        }
        return null;
    }
}
