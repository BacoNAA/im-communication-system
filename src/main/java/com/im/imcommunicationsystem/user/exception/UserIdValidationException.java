package com.im.imcommunicationsystem.user.exception;

/**
 * 个人ID验证异常
 * 用于处理个人ID格式不正确的情况
 */
public class UserIdValidationException extends RuntimeException {

    private final String userIdString;

    public UserIdValidationException(String userIdString) {
        super(String.format("个人ID格式不正确：'%s'。个人ID必须为3-20位字符，只能包含字母、数字和下划线，不能包含特殊符号或空格。", userIdString));
        this.userIdString = userIdString;
    }

    public UserIdValidationException(String message, String userIdString) {
        super(message);
        this.userIdString = userIdString;
    }

    public String getUserIdString() {
        return userIdString;
    }

    /**
     * 创建格式错误异常的静态方法
     */
    public static UserIdValidationException invalidFormat(String userIdString) {
        return new UserIdValidationException(userIdString);
    }

    /**
     * 创建包含敏感词异常的静态方法
     */
    public static UserIdValidationException containsSensitiveWords(String userIdString) {
        return new UserIdValidationException(
            String.format("个人ID '%s' 包含敏感词，请重新设置。个人ID必须为3-20位字符，只能包含字母、数字和下划线。", userIdString),
            userIdString
        );
    }
}