package com.im.imcommunicationsystem.common.exception;

/**
 * 资源未找到异常
 * 用于处理请求的资源不存在的情况
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 