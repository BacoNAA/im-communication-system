package com.im.imcommunicationsystem.moment.dto.request;

import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新动态请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMomentRequest {
    
    /**
     * 更新的动态文字内容
     */
    @Size(max = 2000, message = "动态内容不能超过2000个字符")
    private String content;
    
    /**
     * 可见性类型
     */
    private VisibilityType visibilityType;
    
    /**
     * 可见性规则
     */
    private CreateMomentRequest.VisibilityRules visibilityRules;
} 