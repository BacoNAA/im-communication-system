package com.im.imcommunicationsystem.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常类
 * 用于在业务逻辑中主动抛出预期的错误
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}