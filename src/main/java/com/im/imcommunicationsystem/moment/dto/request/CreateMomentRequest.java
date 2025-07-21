package com.im.imcommunicationsystem.moment.dto.request;

import com.im.imcommunicationsystem.moment.enums.MediaType;
import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建动态请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMomentRequest {
    
    /**
     * 动态文字内容
     */
    @Size(max = 2000, message = "动态内容不能超过2000个字符")
    private String content;
    
    /**
     * 媒体文件URL列表
     */
    private List<String> mediaUrls;
    
    /**
     * 媒体类型
     */
    @NotNull(message = "媒体类型不能为空")
    private MediaType mediaType;
    
    /**
     * 可见性类型
     */
    @NotNull(message = "可见性类型不能为空")
    private VisibilityType visibilityType;
    
    /**
     * 可见性规则
     */
    private VisibilityRules visibilityRules;
    
    /**
     * 可见性规则内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilityRules {
        /**
         * 允许查看的用户ID列表
         */
        private List<Long> allowedUserIds;
        
        /**
         * 不允许查看的用户ID列表
         */
        private List<Long> blockedUserIds;
    }
} 