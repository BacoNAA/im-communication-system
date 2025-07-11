package com.im.imcommunicationsystem.relationship.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 搜索联系人请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "搜索联系人请求")
public class SearchContactRequest {

    /**
     * 搜索关键词（用户ID或昵称）
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(min = 1, max = 50, message = "搜索关键词长度必须在1-50个字符之间")
    @Schema(description = "搜索关键词（用户ID或昵称）", example = "user123")
    private String keyword;

    /**
     * 搜索类型：ID或昵称
     */
    @Schema(description = "搜索类型", example = "ID", allowableValues = {"ID", "NICKNAME"})
    private String searchType = "ID";

    /**
     * 当前用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "当前用户ID", example = "1")
    private Long userId;
}