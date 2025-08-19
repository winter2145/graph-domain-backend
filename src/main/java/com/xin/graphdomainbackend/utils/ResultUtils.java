package com.xin.graphdomainbackend.utils;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.ErrorCode;

/**
 * 自定义返回结果工具类
 */
public class ResultUtils {

    // 成功
    public static<T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    // errorCode的错误信息  <?> 避免了需要传入一个不必要的类型
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    // 自定义errorCode的错误信息
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    // 自定义code与错误信息
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, message);
    }

}

