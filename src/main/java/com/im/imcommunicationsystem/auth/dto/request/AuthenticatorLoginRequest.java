package com.im.imcommunicationsystem.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 验证器登录请求DTO（二次验证）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatorLoginRequest {

    /**
     * 临时令牌（首次登录后获得）
     */
    @NotBlank(message = "临时令牌不能为空")
    private String tempToken;

    /**
     * 验证器代码
     */
    @NotBlank(message = "验证器代码不能为空")
    @Size(min = 6, max = 6, message = "验证器代码必须为6位")
    private String authenticatorCode;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备信息
     */
    private String deviceInfo;
}