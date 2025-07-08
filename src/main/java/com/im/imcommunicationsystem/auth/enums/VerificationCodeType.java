package com.im.imcommunicationsystem.auth.enums;

/**
 * 验证码类型枚举
 */
public enum VerificationCodeType {
    /**
     * 注册验证码
     */
    register,
    
    /**
     * 登录验证码
     */
    login,
    
    /**
     * 重置密码验证码
     */
    reset_password
}