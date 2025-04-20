package com.xin.graphdomainbackend.exception;

import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException{

    // 错误码
    private final int code;

    // errorCode的异常信息
    public BusinessException(ErrorCode errorCode) {
        // super 调用父类的构造方法
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    // 自定义异常信息
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
