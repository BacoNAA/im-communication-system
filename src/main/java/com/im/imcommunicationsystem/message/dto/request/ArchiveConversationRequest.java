package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 归档会话请求DTO
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-07-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveConversationRequest {

    /**
     * 是否归档
     * true表示归档，false表示取消归档
     */
    private Boolean isArchived;
}