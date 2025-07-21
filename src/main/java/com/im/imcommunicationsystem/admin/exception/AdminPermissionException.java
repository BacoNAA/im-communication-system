package com.im.imcommunicationsystem.admin.exception;

/**
 * Exception thrown when an admin doesn't have permission for an operation
 */
public class AdminPermissionException extends RuntimeException {

    /**
     * Constructs a new admin permission exception with the specified detail message
     *
     * @param message the detail message
     */
    public AdminPermissionException(String message) {
        super(message);
    }

    /**
     * Constructs a new admin permission exception with the specified detail message and cause
     *
     * @param message the detail message
     * @param cause the cause
     */
    public AdminPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
} 