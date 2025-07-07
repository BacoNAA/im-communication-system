package com.im.imcommunicationsystem.common.constants;

/**
 * 全局错误码常量定义
 * 建议按模块或业务范围进行分组
 */
public interface ErrorCodes {

    // --- 通用错误码 1000-1999 ---
    int INVALID_PARAMETER = 1001; // 参数无效
    int RESOURCE_NOT_FOUND = 1004; // 资源未找到

    // --- 认证/用户模块错误码 2000-2999 ---
    int USER_NOT_FOUND = 2001;
    int PASSWORD_INCORRECT = 2002;
    int EMAIL_ALREADY_EXISTS = 2003;
    int VERIFICATION_CODE_ERROR = 2004; // 验证码错误或失效

    // ... 后续可添加其他模块的错误码
}