package com.im.imcommunicationsystem.message.exception;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 参数验证异常类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class ValidationException extends MessageException {

    /**
     * 验证错误详情
     */
    private final Map<String, List<String>> validationErrors;

    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.validationErrors = new HashMap<>();
    }

    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param validationErrors 验证错误详情
     */
    public ValidationException(String message, Map<String, List<String>> validationErrors) {
        super("VALIDATION_ERROR", message);
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
    }

    /**
     * 构造函数
     * 
     * @param field 字段名
     * @param error 错误信息
     */
    public ValidationException(String field, String error) {
        super("VALIDATION_ERROR", String.format("字段 %s 验证失败: %s", field, error));
        this.validationErrors = new HashMap<>();
        this.validationErrors.put(field, List.of(error));
    }

    /**
     * 获取验证错误详情
     * 
     * @return 验证错误详情
     */
    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    /**
     * 添加验证错误
     * 
     * @param field 字段名
     * @param error 错误信息
     */
    public void addValidationError(String field, String error) {
        validationErrors.computeIfAbsent(field, k -> new ArrayList<>()).add(error);
    }

    /**
     * 检查是否有验证错误
     * 
     * @return 是否有验证错误
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * 获取指定字段的错误信息
     * 
     * @param field 字段名
     * @return 错误信息列表
     */
    public List<String> getFieldErrors(String field) {
        return validationErrors.getOrDefault(field, new ArrayList<>());
    }

    /**
     * 获取所有错误信息
     * 
     * @return 所有错误信息
     */
    public List<String> getAllErrors() {
        List<String> allErrors = new ArrayList<>();
        for (List<String> errors : validationErrors.values()) {
            allErrors.addAll(errors);
        }
        return allErrors;
    }

    /**
     * 参数为空异常
     * 
     * @param paramName 参数名
     * @return 异常实例
     */
    public static ValidationException parameterRequired(String paramName) {
        return new ValidationException(paramName, "参数不能为空");
    }

    /**
     * 参数格式错误异常
     * 
     * @param paramName 参数名
     * @param expectedFormat 期望格式
     * @return 异常实例
     */
    public static ValidationException invalidFormat(String paramName, String expectedFormat) {
        return new ValidationException(paramName, 
                                     String.format("参数格式错误，期望格式: %s", expectedFormat));
    }

    /**
     * 参数长度超限异常
     * 
     * @param paramName 参数名
     * @param maxLength 最大长度
     * @return 异常实例
     */
    public static ValidationException lengthExceeded(String paramName, int maxLength) {
        return new ValidationException(paramName, 
                                     String.format("参数长度不能超过 %d 个字符", maxLength));
    }

    /**
     * 参数长度不足异常
     * 
     * @param paramName 参数名
     * @param minLength 最小长度
     * @return 异常实例
     */
    public static ValidationException lengthInsufficient(String paramName, int minLength) {
        return new ValidationException(paramName, 
                                     String.format("参数长度不能少于 %d 个字符", minLength));
    }

    /**
     * 参数值超出范围异常
     * 
     * @param paramName 参数名
     * @param minValue 最小值
     * @param maxValue 最大值
     * @return 异常实例
     */
    public static ValidationException valueOutOfRange(String paramName, Object minValue, Object maxValue) {
        return new ValidationException(paramName, 
                                     String.format("参数值必须在 %s 到 %s 之间", minValue, maxValue));
    }

    /**
     * 参数值无效异常
     * 
     * @param paramName 参数名
     * @param validValues 有效值列表
     * @return 异常实例
     */
    public static ValidationException invalidValue(String paramName, List<String> validValues) {
        return new ValidationException(paramName, 
                                     String.format("参数值无效，有效值: %s", String.join(", ", validValues)));
    }

    /**
     * 邮箱格式错误异常
     * 
     * @param email 邮箱地址
     * @return 异常实例
     */
    public static ValidationException invalidEmail(String email) {
        return new ValidationException("email", 
                                     String.format("邮箱格式错误: %s", email));
    }

    /**
     * 手机号格式错误异常
     * 
     * @param phone 手机号
     * @return 异常实例
     */
    public static ValidationException invalidPhone(String phone) {
        return new ValidationException("phone", 
                                     String.format("手机号格式错误: %s", phone));
    }

    /**
     * URL格式错误异常
     * 
     * @param url URL地址
     * @return 异常实例
     */
    public static ValidationException invalidUrl(String url) {
        return new ValidationException("url", 
                                     String.format("URL格式错误: %s", url));
    }

    /**
     * 日期格式错误异常
     * 
     * @param date 日期字符串
     * @param expectedFormat 期望格式
     * @return 异常实例
     */
    public static ValidationException invalidDate(String date, String expectedFormat) {
        return new ValidationException("date", 
                                     String.format("日期格式错误: %s，期望格式: %s", date, expectedFormat));
    }

    /**
     * 时间范围错误异常
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常实例
     */
    public static ValidationException invalidTimeRange(String startTime, String endTime) {
        return new ValidationException("timeRange", 
                                     String.format("时间范围错误: 开始时间 %s 不能晚于结束时间 %s", startTime, endTime));
    }

    /**
     * 分页参数错误异常
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 异常实例
     */
    public static ValidationException invalidPagination(int page, int size) {
        return new ValidationException("pagination", 
                                     String.format("分页参数错误: 页码 %d 和每页大小 %d 必须大于0", page, size));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ValidationException{errorCode='%s', message='%s'", 
                               getErrorCode(), getMessage()));
        if (hasValidationErrors()) {
            sb.append(", validationErrors={");
            validationErrors.forEach((field, errors) -> 
                sb.append(String.format("%s: [%s], ", field, String.join(", ", errors))));
            sb.append("}}");
        } else {
            sb.append("}");
        }
        return sb.toString();
    }
}