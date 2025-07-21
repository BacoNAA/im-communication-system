package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 群组搜索请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchRequest {
    
    /**
     * 关键词（群组名称或ID）
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
    
    /**
     * 页码
     */
    @Min(value = 0, message = "页码不能小于0")
    private int page = 0;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    private int size = 20;
} 