package com.im.imcommunicationsystem.relationship.exception;

import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;

/**
 * 好友请求相关异常类
 * 用于处理好友请求管理中的业务异常
 */
public class ContactRequestException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 请求ID
     */
    private final Long requestId;

    /**
     * 请求者ID
     */
    private final Long requesterId;

    /**
     * 接收者ID
     */
    private final Long recipientId;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ContactRequestException(String message) {
        super(message);
        this.errorCode = "REQUEST_ERROR";
        this.requestId = null;
        this.requesterId = null;
        this.recipientId = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public ContactRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = null;
        this.requesterId = null;
        this.recipientId = null;
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param requestId 请求ID
     * @param requesterId 请求者ID
     * @param recipientId 接收者ID
     */
    public ContactRequestException(String errorCode, String message, Long requestId, Long requesterId, Long recipientId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
    }

    /**
     * 好友请求不存在异常
     */
    public static ContactRequestException requestNotFound(Long requestId) {
        return new ContactRequestException(
            "REQUEST_NOT_FOUND",
            String.format("好友请求不存在: 请求ID=%d", requestId),
            requestId, null, null
        );
    }

    /**
     * 好友请求已存在异常
     */
    public static ContactRequestException requestAlreadyExists(Long requesterId, Long recipientId) {
        return new ContactRequestException(
            "REQUEST_ALREADY_EXISTS",
            String.format("好友请求已存在: 请求者ID=%d, 接收者ID=%d", requesterId, recipientId),
            null, requesterId, recipientId
        );
    }

    /**
     * 不能向自己发送好友请求异常
     */
    public static ContactRequestException cannotRequestSelf(Long userId) {
        return new ContactRequestException(
            "CANNOT_REQUEST_SELF",
            "不能向自己发送好友请求",
            null, userId, userId
        );
    }

    /**
     * 好友请求状态无效异常
     */
    public static ContactRequestException invalidStatus(Long requestId, ContactRequestStatus currentStatus, ContactRequestStatus expectedStatus) {
        return new ContactRequestException(
            "INVALID_REQUEST_STATUS",
            String.format("好友请求状态无效: 请求ID=%d, 当前状态=%s, 期望状态=%s", requestId, currentStatus, expectedStatus),
            requestId, null, null
        );
    }

    /**
     * 无权限操作好友请求异常
     */
    public static ContactRequestException noPermission(Long requestId, Long userId) {
        return new ContactRequestException(
            "NO_PERMISSION",
            String.format("无权限操作该好友请求: 请求ID=%d, 用户ID=%d", requestId, userId),
            requestId, null, null
        );
    }

    /**
     * 好友请求已过期异常
     */
    public static ContactRequestException requestExpired(Long requestId) {
        return new ContactRequestException(
            "REQUEST_EXPIRED",
            String.format("好友请求已过期: 请求ID=%d", requestId),
            requestId, null, null
        );
    }

    /**
     * 已经是好友关系异常
     */
    public static ContactRequestException alreadyFriends(Long requesterId, Long recipientId) {
        return new ContactRequestException(
            "ALREADY_FRIENDS",
            String.format("用户已经是好友关系: 请求者ID=%d, 接收者ID=%d", requesterId, recipientId),
            null, requesterId, recipientId
        );
    }

    /**
     * 用户被屏蔽异常
     */
    public static ContactRequestException userBlocked(Long requesterId, Long recipientId) {
        return new ContactRequestException(
            "USER_BLOCKED",
            String.format("用户被屏蔽，无法发送好友请求: 请求者ID=%d, 接收者ID=%d", requesterId, recipientId),
            null, requesterId, recipientId
        );
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getRecipientId() {
        return recipientId;
    }
}