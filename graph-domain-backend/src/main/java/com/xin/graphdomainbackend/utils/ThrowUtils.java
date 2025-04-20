package com.xin.graphdomainbackend.utils;

import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;

/**
 * 异常处理工具类
 */
public class ThrowUtils {

    // 条件成立则抛异常
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    // 条件成立,则抛errorCode的错误信息
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throwIf(true, new BusinessException(errorCode));
        }
    }

    // 条件成立,则抛自定义的错误异常信息
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throwIf(true, new BusinessException(errorCode, message));
        }
    }
}
