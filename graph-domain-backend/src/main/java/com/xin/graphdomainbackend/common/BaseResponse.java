package com.xin.graphdomainbackend.common;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.xin.graphdomainbackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    // 自定义errorCode的错误信息
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    // errorCode的错误信息
    public BaseResponse(ErrorCode errorCode) {
        // this 调用自己的构造方法
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    // 自定义code与错误信息
    public BaseResponse(int code, String message) {
        this(code, null, message);
    }

}
