package com.im.imcommunicationsystem.user.exception;

/**
 * 用户未找到异常
 */
public class UserNotFoundException extends RuntimeException {

    private final Long userId;
    private final String userIdString;

    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
        this.userId = userId;
        this.userIdString = null;
    }

    public UserNotFoundException(String userIdString) {
        super("User not found with user ID string: " + userIdString);
        this.userId = null;
        this.userIdString = userIdString;
    }

    public UserNotFoundException(String message, Long userId) {
        super(message);
        this.userId = userId;
        this.userIdString = null;
    }

    public UserNotFoundException(String message, String userIdString) {
        super(message);
        this.userId = null;
        this.userIdString = userIdString;
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.userIdString = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserIdString() {
        return userIdString;
    }
}