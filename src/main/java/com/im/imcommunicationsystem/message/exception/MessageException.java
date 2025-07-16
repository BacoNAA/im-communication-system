package com.im.imcommunicationsystem.message.exception;

/**
 * 消息模块基础异常类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class MessageException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public MessageException(String message) {
        super(message);
        this.errorCode = "MESSAGE_ERROR";
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public MessageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MESSAGE_ERROR";
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public MessageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public MessageException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("MessageException{errorCode='%s', message='%s'}", 
                           errorCode, getMessage());
    }
}