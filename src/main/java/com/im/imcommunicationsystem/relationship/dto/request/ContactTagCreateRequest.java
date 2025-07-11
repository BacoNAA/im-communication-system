package com.im.imcommunicationsystem.relationship.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 好友标签创建请求DTO
 */
@Data
public class ContactTagCreateRequest {

    /**
     * 标签所有者ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 20, message = "标签名称长度必须在1-20个字符之间")
    private String name;

    /**
     * 标签颜色（十六进制颜色代码）
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色格式必须为十六进制颜色代码，如#FF0000")
    private String color;
}