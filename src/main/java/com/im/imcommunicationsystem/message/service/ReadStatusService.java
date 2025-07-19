package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.entity.ReadStatus;

import java.util.List;

/**
 * 已读状态服务接口
 * 定义已读状态相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-07-18
 */
public interface ReadStatusService {

    /**
     * 更新用户在会话中的已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param lastReadMessageId 最后已读消息ID
     * @return 更新后的已读状态
     */
    ReadStatus updateReadStatus(Long userId, Long conversationId, Long lastReadMessageId);

    /**
     * 获取用户在会话中的已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 已读状态，如果不存在则返回null
     */
    ReadStatus getReadStatus(Long userId, Long conversationId);

    /**
     * 获取会话中所有用户的已读状态
     * 
     * @param conversationId 会话ID
     * @return 已读状态列表
     */
    List<ReadStatus> getReadStatusByConversation(Long conversationId);

    /**
     * 获取用户在所有会话中的已读状态
     * 
     * @param userId 用户ID
     * @return 已读状态列表
     */
    List<ReadStatus> getReadStatusByUser(Long userId);

    /**
     * 计算会话中的未读消息数量
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 未读消息数量
     */
    Long countUnreadMessages(Long userId, Long conversationId);

    /**
     * 标记会话中的所有消息为已读
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 更新后的已读状态
     */
    ReadStatus markAllAsRead(Long userId, Long conversationId);

    /**
     * 删除用户在会话中的已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    void deleteReadStatus(Long userId, Long conversationId);
} 