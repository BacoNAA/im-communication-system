package com.im.imcommunicationsystem.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 验证码登录请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeLoginRequest {

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须为6位")
    private String verificationCode;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 是否记住登录状态
     */
    private Boolean rememberMe = false;
}