package com.im.imcommunicationsystem.message.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 会话更新事件
 * 用于解耦WebSocketService和ConversationService之间的循环依赖
 */
@Getter
public class ConversationUpdateEvent extends ApplicationEvent {
    
    private final Long conversationId;
    private final Object conversationData;
    private final String updateType;
    private final Long excludeUserId;
    
    /**
     * 创建会话更新事件
     * 
     * @param source 事件源
     * @param conversationId 会话ID
     * @param conversationData 会话数据
     * @param updateType 更新类型
     * @param excludeUserId 排除的用户ID（可选）
     */
    public ConversationUpdateEvent(Object source, Long conversationId, Object conversationData, 
                                   String updateType, Long excludeUserId) {
        super(source);
        this.conversationId = conversationId;
        this.conversationData = conversationData;
        this.updateType = updateType;
        this.excludeUserId = excludeUserId;
    }
} 