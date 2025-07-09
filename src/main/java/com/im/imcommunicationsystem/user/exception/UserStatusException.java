package com.im.imcommunicationsystem.user.exception;

/**
 * 用户状态异常
 */
public class UserStatusException extends RuntimeException {

    private final Long userId;
    private final String statusType;
    private final String statusOperation;

    public UserStatusException(String message) {
        super(message);
        this.userId = null;
        this.statusType = null;
        this.statusOperation = null;
    }

    public UserStatusException(String message, Long userId) {
        super(message);
        this.userId = userId;
        this.statusType = null;
        this.statusOperation = null;
    }

    public UserStatusException(String message, Long userId, String statusType) {
        super(message);
        this.userId = userId;
        this.statusType = statusType;
        this.statusOperation = null;
    }

    public UserStatusException(String message, Long userId, String statusType, String statusOperation) {
        super(message);
        this.userId = userId;
        this.statusType = statusType;
        this.statusOperation = statusOperation;
    }

    public UserStatusException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.statusType = null;
        this.statusOperation = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStatusType() {
        return statusType;
    }

    public String getStatusOperation() {
        return statusOperation;
    }

    /**
     * 创建状态内容过长异常
     */
    public static UserStatusException contentTooLong(Long userId, int currentLength, int maxLength) {
        return new UserStatusException(
            String.format("Status content too long for user %d: %d characters (max: %d)", 
                userId, currentLength, maxLength),
            userId, null, "SET"
        );
    }

    /**
     * 创建状态类型无效异常
     */
    public static UserStatusException invalidStatusType(Long userId, String statusType) {
        return new UserStatusException(
            String.format("Invalid status type '%s' for user %d", statusType, userId),
            userId, statusType, "SET"
        );
    }

    /**
     * 创建状态已过期异常
     */
    public static UserStatusException statusExpired(Long userId) {
        return new UserStatusException(
            String.format("Status has expired for user %d", userId),
            userId, null, "GET"
        );
    }

    /**
     * 创建状态不存在异常
     */
    public static UserStatusException statusNotFound(Long userId) {
        return new UserStatusException(
            String.format("No status found for user %d", userId),
            userId, null, "GET"
        );
    }

    /**
     * 创建状态更新失败异常
     */
    public static UserStatusException updateFailed(Long userId, Throwable cause) {
        return new UserStatusException(
            String.format("Failed to update status for user %d", userId),
            cause
        );
    }
}