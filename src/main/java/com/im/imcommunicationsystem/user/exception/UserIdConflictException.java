package com.im.imcommunicationsystem.user.exception;

/**
 * 用户ID冲突异常
 */
public class UserIdConflictException extends RuntimeException {

    private final String userIdString;
    private final Long existingUserId;

    public UserIdConflictException(String userIdString) {
        super("User ID already exists: " + userIdString);
        this.userIdString = userIdString;
        this.existingUserId = null;
    }

    public UserIdConflictException(String userIdString, Long existingUserId) {
        super(String.format("User ID '%s' already exists and is used by user with ID: %d", userIdString, existingUserId));
        this.userIdString = userIdString;
        this.existingUserId = existingUserId;
    }

    public UserIdConflictException(String message, String userIdString) {
        super(message);
        this.userIdString = userIdString;
        this.existingUserId = null;
    }

    public UserIdConflictException(String message, Throwable cause) {
        super(message, cause);
        this.userIdString = null;
        this.existingUserId = null;
    }

    public String getUserIdString() {
        return userIdString;
    }

    public Long getExistingUserId() {
        return existingUserId;
    }

    /**
     * 创建用户ID已存在异常
     */
    public static UserIdConflictException alreadyExists(String userIdString, Long existingUserId) {
        return new UserIdConflictException(userIdString, existingUserId);
    }

    /**
     * 创建用户ID格式无效异常
     */
    public static UserIdConflictException invalidFormat(String userIdString) {
        return new UserIdConflictException(
            String.format("Invalid user ID format: %s. User ID must be 3-20 characters long and contain only letters, numbers, and underscores.", userIdString),
            userIdString
        );
    }

    /**
     * 创建用户ID包含敏感词异常
     */
    public static UserIdConflictException containsSensitiveWords(String userIdString) {
        return new UserIdConflictException(
            String.format("User ID contains sensitive words: %s", userIdString),
            userIdString
        );
    }
}