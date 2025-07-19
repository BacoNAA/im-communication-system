package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话免打扰请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MuteConversationRequest {
    
    /**
     * 是否开启免打扰
     */
    private Boolean isMuted;
} 