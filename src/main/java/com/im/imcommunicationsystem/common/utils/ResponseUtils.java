package com.im.imcommunicationsystem.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 响应工具类
 * 统一API响应格式
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public class ResponseUtils {

    /**
     * 成功响应 - 无数据
     * 
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    /**
     * 成功响应 - 带数据
     * 
     * @param data 响应数据
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    /**
     * 成功响应 - 自定义消息
     * 
     * @param message 响应消息
     * @param data 响应数据
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 错误响应 - 默认错误
     * 
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(500, "操作失败", null);
    }

    /**
     * 错误响应 - 自定义消息
     * 
     * @param message 错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 错误响应 - 自定义错误码和消息
     * 
     * @param code 错误码
     * @param message 错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 错误响应 - 自定义错误码、消息和数据
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误数据
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(Integer code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    /**
     * 参数验证失败响应
     * 
     * @param message 验证失败消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> validationError(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /**
     * 参数验证失败响应 - 带详细错误信息
     * 
     * @param message 验证失败消息
     * @param errors 详细错误信息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> validationError(String message, T errors) {
        return new ApiResponse<>(400, message, errors);
    }

    /**
     * 未授权响应
     * 
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "未授权访问，请先登录", null);
    }

    /**
     * 未授权响应 - 自定义消息
     * 
     * @param message 未授权消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }

    /**
     * 权限不足响应
     * 
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "权限不足，拒绝访问", null);
    }

    /**
     * 权限不足响应 - 自定义消息
     * 
     * @param message 权限不足消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message, null);
    }

    /**
     * 资源不存在响应
     * 
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> notFound() {
        return new ApiResponse<>(404, "请求的资源不存在", null);
    }

    /**
     * 资源不存在响应 - 自定义消息
     * 
     * @param message 资源不存在消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    /**
     * 分页响应
     * 
     * @param data 分页数据
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<PageResult<T>> page(T data, Long total, Integer page, Integer size) {
        PageResult<T> pageResult = new PageResult<>(data, total, page, size);
        return success("查询成功", pageResult);
    }

    /**
     * API响应类
     * 
     * @param <T> 数据类型
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        /**
         * 响应码
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
         * 响应时间
         */
        private LocalDateTime timestamp;

        /**
         * 构造函数
         */
        public ApiResponse() {
            this.timestamp = LocalDateTime.now();
        }

        /**
         * 构造函数
         * 
         * @param code 响应码
         * @param message 响应消息
         * @param data 响应数据
         */
        public ApiResponse(Integer code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        /**
         * 判断是否成功
         * 
         * @return 是否成功
         */
        public boolean isSuccess() {
            return this.code != null && this.code == 200;
        }
    }

    /**
     * 分页结果类
     * 
     * @param <T> 数据类型
     */
    @Data
    public static class PageResult<T> {
        /**
         * 数据列表
         */
        private T list;

        /**
         * 总记录数
         */
        private Long total;

        /**
         * 当前页码
         */
        private Integer page;

        /**
         * 每页大小
         */
        private Integer size;

        /**
         * 总页数
         */
        private Integer pages;

        /**
         * 构造函数
         * 
         * @param list 数据列表
         * @param total 总记录数
         * @param page 当前页码
         * @param size 每页大小
         */
        public PageResult(T list, Long total, Integer page, Integer size) {
            this.list = list;
            this.total = total;
            this.page = page;
            this.size = size;
            this.pages = (int) Math.ceil((double) total / size);
        }
    }

    /**
     * 响应码定义：
     * 
     * 2xx 成功状态码：
     * - 200: 操作成功
     * - 201: 创建成功
     * - 204: 删除成功（无内容）
     * 
     * 4xx 客户端错误：
     * - 400: 请求参数错误
     * - 401: 未授权（未登录）
     * - 403: 权限不足（已登录但无权限）
     * - 404: 资源不存在
     * - 409: 资源冲突
     * - 422: 参数验证失败
     * 
     * 5xx 服务器错误：
     * - 500: 服务器内部错误
     * - 502: 网关错误
     * - 503: 服务不可用
     * 
     * 业务错误码（1000+）：
     * - 1001-1099: 用户相关错误
     * - 1100-1199: 认证相关错误
     * - 1200-1299: 消息相关错误
     * - 1300-1399: 群组相关错误
     * - 1400-1499: 文件相关错误
     * 
     * 使用示例：
     * 
     * // 成功响应
     * return ResponseUtils.success(userInfo);
     * return ResponseUtils.success("登录成功", token);
     * 
     * // 错误响应
     * return ResponseUtils.error("用户不存在");
     * return ResponseUtils.error(1001, "用户名已存在");
     * 
     * // 分页响应
     * return ResponseUtils.page(userList, total, page, size);
     * 
     * // 验证失败
     * return ResponseUtils.validationError("参数验证失败", errors);
     */

}