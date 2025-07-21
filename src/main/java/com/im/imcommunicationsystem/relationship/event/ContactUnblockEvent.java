package com.im.imcommunicationsystem.relationship.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 联系人解除拉黑事件
 * 当用户解除拉黑联系人时触发
 * 用于解耦ContactService和ConversationService之间的依赖
 * 
 * 解除拉黑机制:
 * 1. 当用户A解除对用户B的拉黑时，系统会将会话成员表中A的last_acceptable_message_id设置为null
 * 2. 这样A就能重新收到B发送的所有新消息
 * 3. 注意：拉黑期间B发送的消息不会自动显示给A，但这些消息仍然保存在数据库中
 * 4. 解除拉黑只影响未来的消息传递，不会自动加载历史消息
 */
@Getter
public class ContactUnblockEvent extends ApplicationEvent {

    private final Long userId;
    private final Long friendId;

    /**
     * 创建联系人解除拉黑事件
     *
     * @param source 事件源
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    public ContactUnblockEvent(Object source, Long userId, Long friendId) {
        super(source);
        this.userId = userId;
        this.friendId = friendId;
    }
} 