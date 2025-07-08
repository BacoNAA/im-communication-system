package com.im.imcommunicationsystem.auth.exception;

/**
 * 认证异常
 * 用于处理认证相关的异常情况
 */
public class AuthenticationException extends RuntimeException {

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 构造函数
     * @param message 错误消息
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * @param message 错误消息
     * @param cause 原因
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public AuthenticationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public AuthenticationException(String errorCode, String message, Throwable cause) {
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
    
    /**
     * 设置错误代码
     * @param errorCode 错误代码
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * 是否有错误代码
     * @return 是否有错误代码
     */
    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.trim().isEmpty();
    }
}