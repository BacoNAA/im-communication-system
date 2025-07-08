package com.im.imcommunicationsystem.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 统一API响应格式
 * 
 * @param <T> 响应数据类型
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    /**
     * 响应状态码
     * 200: 成功
     * 400: 客户端错误
     * 500: 服务器错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 请求追踪ID
     */
    private String traceId;

    /**
     * 创建成功响应
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（无数据）
     * 
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 创建成功响应（自定义消息）
     * 
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 创建失败响应
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建客户端错误响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 客户端错误响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 创建服务器错误响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 服务器错误响应
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }

    /**
     * 创建未授权响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }

    /**
     * 创建禁止访问响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error(403, message);
    }

    /**
     * 创建资源未找到响应
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 资源未找到响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }

    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }

    /**
     * 设置追踪ID
     * 
     * @param traceId 追踪ID
     * @return 当前对象
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}