package com.im.imcommunicationsystem.admin.exception;

/**
 * Exception thrown when content moderation fails
 */
public class ContentModerationException extends RuntimeException {

    /**
     * Constructs a new content moderation exception with the specified detail message
     *
     * @param message the detail message
     */
    public ContentModerationException(String message) {
        super(message);
    }

    /**
     * Constructs a new content moderation exception with the specified detail message and cause
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ContentModerationException(String message, Throwable cause) {
        super(message, cause);
    }
} 