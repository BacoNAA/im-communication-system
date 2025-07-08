package com.im.imcommunicationsystem.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 登出请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 是否清除记住的登录信息
     */
    private Boolean clearRememberedInfo = false;
}