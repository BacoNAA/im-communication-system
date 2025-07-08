package com.im.imcommunicationsystem.auth.exception;

/**
 * 业务异常
 * 用于处理业务逻辑相关的异常情况
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 构造函数
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * @param message 错误消息
     * @param cause 原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
}