package com.im.imcommunicationsystem.common.exception;

/**
 * 业务服务异常类
 * 用于处理业务逻辑中的异常情况
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public class ServiceException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误详情
     */
    private final Object errorDetails;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ServiceException(String message) {
        super(message);
        this.errorCode = "SERVICE_ERROR";
        this.errorDetails = null;
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因异常
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SERVICE_ERROR";
        this.errorDetails = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public ServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原因异常
     */
    public ServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetails = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param errorDetails 错误详情
     */
    public ServiceException(String errorCode, String message, Object errorDetails) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原因异常
     * @param errorDetails 错误详情
     */
    public ServiceException(String errorCode, String message, Throwable cause, Object errorDetails) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误详情
     * 
     * @return 错误详情
     */
    public Object getErrorDetails() {
        return errorDetails;
    }

    /**
     * 创建验证码相关异常
     * 
     * @param message 错误消息
     * @return ServiceException实例
     */
    public static ServiceException verificationCodeError(String message) {
        return new ServiceException("VERIFICATION_CODE_ERROR", message);
    }

    /**
     * 创建Redis连接异常
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @return ServiceException实例
     */
    public static ServiceException redisConnectionError(String message, Throwable cause) {
        return new ServiceException("REDIS_CONNECTION_ERROR", message, cause);
    }

    /**
     * 创建参数验证异常
     * 
     * @param message 错误消息
     * @param details 验证详情
     * @return ServiceException实例
     */
    public static ServiceException validationError(String message, Object details) {
        return new ServiceException("VALIDATION_ERROR", message, details);
    }

    /**
     * 创建资源不存在异常
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return ServiceException实例
     */
    public static ServiceException resourceNotFound(String resourceType, Object resourceId) {
        String message = String.format("%s not found: %s", resourceType, resourceId);
        return new ServiceException("RESOURCE_NOT_FOUND", message, resourceId);
    }

    /**
     * 创建权限不足异常
     * 
     * @param operation 操作名称
     * @return ServiceException实例
     */
    public static ServiceException accessDenied(String operation) {
        return new ServiceException("ACCESS_DENIED", "Access denied for operation: " + operation);
    }

    /**
     * 创建频率限制异常
     * 
     * @param operation 操作名称
     * @param remainingTime 剩余等待时间
     * @return ServiceException实例
     */
    public static ServiceException rateLimitExceeded(String operation, long remainingTime) {
        String message = String.format("Rate limit exceeded for %s, please wait %d seconds", operation, remainingTime);
        return new ServiceException("RATE_LIMIT_EXCEEDED", message, remainingTime);
    }
}