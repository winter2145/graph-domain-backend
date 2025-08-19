package com.xin.graphdomainbackend.utils;

import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import org.jetbrains.annotations.Contract;

/**
 * 异常处理工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     */
    @Contract("true, _ -> fail") // 当 condition == true 时，此方法必定抛异常，不再继续
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛出默认业务异常
     */
    @Contract("true, _ -> fail")
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 条件成立则抛出带 message 的业务异常
     */
    @Contract("true, _, _ -> fail")
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }
}
