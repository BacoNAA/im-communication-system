package com.im.imcommunicationsystem.relationship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 设置好友备注请求DTO
 */
@Data
@Schema(description = "设置好友备注请求")
public class SetAliasRequest {
    
    @Schema(description = "备注名", example = "小明")
    @Size(max = 50, message = "备注长度不能超过50个字符")
    private String alias;
}