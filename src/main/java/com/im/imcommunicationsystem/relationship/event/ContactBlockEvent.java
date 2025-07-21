package com.im.imcommunicationsystem.relationship.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 联系人拉黑事件
 * 当用户拉黑联系人时触发
 * 用于解耦ContactService和ConversationService之间的依赖
 * 
 * 拉黑机制:
 * 1. 当用户A拉黑用户B时，系统会在会话成员表中设置A的last_acceptable_message_id为拉黑时的最后一条消息ID
 * 2. 被拉黑的用户B仍然可以发送消息到A，这些消息会保存在数据库中
 * 3. WebSocketService在广播消息时，如果消息ID大于A的last_acceptable_message_id，则不会将消息发送给A
 * 4. 当A解除拉黑后，A会重新收到B发送的新消息，但拉黑期间的消息不会自动显示
 */
@Getter
public class ContactBlockEvent extends ApplicationEvent {

    private final Long userId;
    private final Long friendId;

    /**
     * 创建联系人拉黑事件
     *
     * @param source 事件源
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    public ContactBlockEvent(Object source, Long userId, Long friendId) {
        super(source);
        this.userId = userId;
        this.friendId = friendId;
    }
} 