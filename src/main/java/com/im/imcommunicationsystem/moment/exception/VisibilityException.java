package com.im.imcommunicationsystem.moment.exception;

/**
 * 可见性权限异常
 */
public class VisibilityException extends RuntimeException {
    
    private Long userId;
    private Long momentId;
    
    public VisibilityException(Long userId, Long momentId) {
        super("用户 " + userId + " 无权访问动态 " + momentId);
        this.userId = userId;
        this.momentId = momentId;
    }
    
    public VisibilityException(String message) {
        super(message);
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public Long getMomentId() {
        return momentId;
    }
} 