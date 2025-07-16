package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.MessageResponse;
import com.im.imcommunicationsystem.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 消息服务接口
 * 定义消息相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface MessageService {

    /**
     * 发送消息
     * 
     * @param request 发送消息请求
     * @param senderId 发送者ID
     * @return 消息响应
     */
    MessageResponse sendMessage(SendMessageRequest request, Long senderId);

    /**
     * 撤回消息
     * 
     * @param messageId 消息ID
     * @param request 撤回消息请求
     * @param userId 用户ID
     */
    void recallMessage(Long messageId, RecallMessageRequest request, Long userId);

    /**
     * 编辑消息
     * 
     * @param messageId 消息ID
     * @param request 编辑消息请求
     * @param userId 用户ID
     * @return 消息响应
     */
    MessageResponse editMessage(Long messageId, EditMessageRequest request, Long userId);

    /**
     * 转发消息
     * 
     * @param request 转发消息请求
     * @param userId 用户ID
     */
    void forwardMessage(ForwardMessageRequest request, Long userId);

    /**
     * 标记消息为已读
     * 
     * @param request 标记已读请求
     * @param userId 用户ID
     */
    void markAsRead(MarkAsReadRequest request, Long userId);

    /**
     * 获取会话消息列表
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 消息列表
     */
    Page<MessageResponse> getConversationMessages(Long conversationId, Long userId, Pageable pageable);

    /**
     * 获取消息历史记录
     * 
     * @param conversationId 会话ID
     * @param beforeMessageId 在此消息之前的消息
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 消息列表
     */
    Page<MessageResponse> getMessageHistory(Long conversationId, Long beforeMessageId, Pageable pageable, Long userId);

    /**
     * 根据ID获取消息
     * 
     * @param messageId 消息ID
     * @return 消息实体
     */
    Message getMessageById(Long messageId);

    /**
     * 批量获取消息
     * 
     * @param messageIds 消息ID列表
     * @return 消息列表
     */
    List<Message> getMessagesByIds(List<Long> messageIds);

    /**
     * 删除消息
     * 
     * @param messageId 消息ID
     * @param userId 用户ID
     */
    void deleteMessage(Long messageId, Long userId);

    /**
     * 获取未读消息数量
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Long getUnreadMessageCount(Long conversationId, Long userId);

    /**
     * 获取会话最后一条消息
     * 
     * @param conversationId 会话ID
     * @return 消息响应
     */
    MessageResponse getLastMessage(Long conversationId);
}