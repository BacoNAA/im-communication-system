package com.im.imcommunicationsystem.common.service;

/**
 * WebSocket服务接口
 * 提供WebSocket消息发送相关功能
 */
public interface WebSocketService {
    
    /**
     * 发送消息给指定用户
     * 
     * @param userId 用户ID
     * @param message 消息内容
     */
    void sendMessageToUser(Long userId, Object message);
    
    /**
     * 发送消息给指定会话的所有成员
     * 
     * @param conversationId 会话ID
     * @param message 消息内容
     * @param excludeUserId 需要排除的用户ID（比如发送者自己）
     */
    void sendMessageToConversation(Long conversationId, Object message, Long excludeUserId);
    
    /**
     * 广播消息给所有连接的用户
     * 
     * @param message 消息内容
     */
    void broadcastMessage(Object message);
    
    /**
     * 发送会话更新通知给指定会话的所有成员
     * 
     * @param conversationId 会话ID
     * @param conversationData 会话数据
     * @param updateType 更新类型 (如: "UPDATE", "NEW", "DELETE", "PIN", "ARCHIVE")
     * @param excludeUserId 需要排除的用户ID（可选）
     */
    void sendConversationUpdate(Long conversationId, Object conversationData, String updateType, Long excludeUserId);
    
    /**
     * 发送群组更新通知给指定群组的所有成员
     * 
     * @param groupId 群组ID
     * @param groupData 群组数据
     * @param updateType 更新类型 (如: "UPDATE", "NEW", "DELETE", "MEMBER_JOIN", "MEMBER_LEAVE")
     * @param excludeUserId 需要排除的用户ID（可选）
     */
    void sendGroupUpdate(Long groupId, Object groupData, String updateType, Long excludeUserId);
} 