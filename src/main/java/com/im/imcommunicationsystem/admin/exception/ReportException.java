package com.im.imcommunicationsystem.admin.exception;

/**
 * 举报处理异常
 */
public class ReportException extends RuntimeException {

    /**
     * 使用指定的错误消息构造一个新的举报异常
     *
     * @param message 错误消息
     */
    public ReportException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造一个新的举报异常
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
} 