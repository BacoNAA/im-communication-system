package com.im.imcommunicationsystem.common.utils;

import lombok.Data;

/**
 * 统一API响应结果封装类
 * @param <T> 响应数据的泛型
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 成功时调用的静态方法
     * @param data 返回的数据内容
     * @return 包含数据的成功响应对象
     * @param <T> 数据的泛型
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("操作成功");
        response.setData(data);
        return response;
    }

    /**
     * 失败时调用的静态方法
     * @param code 自定义错误码
     * @param message 错误信息
     * @return 包含错误信息的响应对象
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}