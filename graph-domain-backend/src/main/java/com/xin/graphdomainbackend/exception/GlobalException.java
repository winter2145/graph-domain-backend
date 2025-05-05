package com.xin.graphdomainbackend.exception;

import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalException {

/*    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> businessExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
    }*/

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);

        HttpStatus status = mapCodeToHttpStatus(e.getCode());

        return new ResponseEntity<>(
                ResultUtils.error(e.getCode(), e.getMessage()),
                status
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return new ResponseEntity<>(
                ResultUtils.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误：" + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }


    /**
     * 根据业务状态码映射 HTTP 状态码
     */
    private HttpStatus mapCodeToHttpStatus(int code) {
        if (code == ErrorCode.SUCCESS.getCode()) return HttpStatus.OK;
        if (code == ErrorCode.PARAMS_ERROR.getCode()) return HttpStatus.BAD_REQUEST;
        if (code == ErrorCode.NOT_LOGIN_ERROR.getCode()) return HttpStatus.UNAUTHORIZED;
        if (code == ErrorCode.NO_AUTH_ERROR.getCode()) return HttpStatus.FORBIDDEN;
        if (code == ErrorCode.NOT_FOUND_ERROR.getCode()) return HttpStatus.NOT_FOUND;
        if (code == ErrorCode.FORBIDDEN_ERROR.getCode()) return HttpStatus.FORBIDDEN;
        if (code == ErrorCode.SYSTEM_ERROR.getCode() || code == ErrorCode.OPERATION_ERROR.getCode()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 默认返回 500
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
