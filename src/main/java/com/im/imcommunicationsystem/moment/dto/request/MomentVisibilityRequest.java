package com.im.imcommunicationsystem.moment.dto.request;

import com.im.imcommunicationsystem.moment.enums.VisibilityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 动态可见性设置请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentVisibilityRequest {
    
    /**
     * 可见性类型
     * PUBLIC: 公开可见（所有好友可见）
     * PRIVATE: 私密（仅自己可见）
     * CUSTOM: 自定义（部分好友可见或部分好友不可见）
     */
    @NotNull(message = "可见性类型不能为空")
    private VisibilityType visibilityType;
    
    /**
     * 允许查看的用户ID列表
     * 当visibilityType为CUSTOM时有效
     */
    private List<Long> allowedUserIds;
    
    /**
     * 不允许查看的用户ID列表
     * 当visibilityType为CUSTOM时有效
     */
    private List<Long> blockedUserIds;
} 