package com.im.imcommunicationsystem.common.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的可预期异常
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数
     * 
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原因异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 静态工厂方法 - 创建业务异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    /**
     * 静态工厂方法 - 创建业务异常
     * 
     * @param code 错误码
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 静态工厂方法 - 用户相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException userError(String message) {
        return new BusinessException(1001, message);
    }

    /**
     * 静态工厂方法 - 认证相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException authError(String message) {
        return new BusinessException(1002, message);
    }

    /**
     * 静态工厂方法 - 消息相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException messageError(String message) {
        return new BusinessException(1003, message);
    }

    /**
     * 静态工厂方法 - 群组相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException groupError(String message) {
        return new BusinessException(1004, message);
    }

    /**
     * 静态工厂方法 - 文件相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException fileError(String message) {
        return new BusinessException(1005, message);
    }

    /**
     * 静态工厂方法 - 权限相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException permissionError(String message) {
        return new BusinessException(1006, message);
    }

    /**
     * 静态工厂方法 - 验证码相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException verificationError(String message) {
        return new BusinessException(1007, message);
    }

    /**
     * 静态工厂方法 - 系统配置相关异常
     * 
     * @param message 错误信息
     * @return BusinessException实例
     */
    public static BusinessException configError(String message) {
        return new BusinessException(1008, message);
    }

    /**
     * 业务异常码定义：
     * 
     * 1000-1099: 通用异常
     * - 1000: 通用业务异常
     * - 1001: 用户相关异常
     * - 1002: 认证相关异常
     * - 1003: 消息相关异常
     * - 1004: 群组相关异常
     * - 1005: 文件相关异常
     * - 1006: 权限相关异常
     * - 1007: 验证码相关异常
     * - 1008: 系统配置相关异常
     * 
     * 1100-1199: 用户模块异常
     * - 1101: 用户不存在
     * - 1102: 用户已存在
     * - 1103: 用户状态异常
     * - 1104: 用户信息不完整
     * 
     * 1200-1299: 认证模块异常
     * - 1201: 登录失败
     * - 1202: Token无效
     * - 1203: Token过期
     * - 1204: 权限不足
     * 
     * 1300-1399: 消息模块异常
     * - 1301: 消息发送失败
     * - 1302: 消息不存在
     * - 1303: 消息已撤回
     * - 1304: 消息格式错误
     * 
     * 1400-1499: 群组模块异常
     * - 1401: 群组不存在
     * - 1402: 群组已满
     * - 1403: 不是群组成员
     * - 1404: 群组权限不足
     * 
     * 1500-1599: 文件模块异常
     * - 1501: 文件上传失败
     * - 1502: 文件不存在
     * - 1503: 文件格式不支持
     * - 1504: 文件大小超限
     * 
     * 使用示例：
     * throw BusinessException.userError("用户不存在");
     * throw BusinessException.authError("登录失败");
     * throw BusinessException.of(1301, "消息发送失败");
     */

}