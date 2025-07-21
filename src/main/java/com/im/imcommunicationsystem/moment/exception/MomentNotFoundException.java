package com.im.imcommunicationsystem.moment.exception;

/**
 * 动态未找到异常
 */
public class MomentNotFoundException extends RuntimeException {
    
    private Long momentId;
    
    public MomentNotFoundException(Long momentId) {
        super("动态不存在: " + momentId);
        this.momentId = momentId;
    }
    
    public MomentNotFoundException(String message) {
        super(message);
    }
    
    public Long getMomentId() {
        return momentId;
    }
} 