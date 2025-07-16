package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.MessageResponse;
import com.im.imcommunicationsystem.message.dto.response.MessageDTO;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.message.service.MessageService;
import com.im.imcommunicationsystem.message.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;

/**
 * 消息服务实现类
 * 实现消息相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final @Lazy ConversationService conversationService;
    private final @Lazy UserProfileService userProfileService;

    @Override
    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        log.info("Sending message from user {} to conversation {}", senderId, request.getConversationId());
        
        try {
            // 1. 验证请求参数
            if (!request.isValid()) {
                log.warn("Invalid send message request from user {}: {}", senderId, request.getValidationErrors());
                return MessageResponse.error("请求参数无效: " + String.join(", ", request.getValidationErrors()));
            }
            
            Conversation conversation = null;
            
            // 2. 处理会话逻辑
            if (request.getConversationId() != null) {
                // 使用现有会话
                conversation = conversationService.getConversationById(request.getConversationId());
                if (conversation == null || conversation.isDeleted()) {
                    log.warn("Conversation {} not found or deleted", request.getConversationId());
                    return MessageResponse.error("会话不存在或已删除");
                }
                
                // 验证用户是否有权限在此会话中发送消息
                if (!conversationService.isUserInConversation(conversation.getId(), senderId)) {
                    log.warn("User {} does not have permission to send message in conversation {}", senderId, conversation.getId());
                    return MessageResponse.error("您没有权限在此会话中发送消息");
                }
            } else if (request.getRecipientId() != null) {
                // 自动创建或获取私聊会话
                conversation = conversationService.getOrCreatePrivateConversation(senderId, request.getRecipientId());
                if (conversation == null) {
                    log.error("Failed to create or get private conversation between users {} and {}", senderId, request.getRecipientId());
                    return MessageResponse.error("创建会话失败");
                }
            } else {
                log.warn("Neither conversationId nor recipientId provided in request from user {}", senderId);
                return MessageResponse.error("必须指定会话ID或接收者ID");
            }
            
            // 3. 创建消息实体
            Message message = Message.builder()
                    .conversationId(conversation.getId())
                    .senderId(senderId) // 确保使用当前发送者的ID，而不是会话创建者ID
                    .messageType(request.getMessageType())
                    .content(request.getContent())
                    .mediaFileId(request.getMediaFileId())
                    .replyToMessageId(request.getReplyToMessageId())
                    .originalMessageId(request.getForwardFromMessageId())
                    .status(MessageStatus.SENT)
                    .edited(false)
                    .indexed(false)
                    .metadata(request.getMetadata())
                    .build();
            
            // 4. 保存消息到数据库
            message = messageRepository.save(message);
            
            // 5. 更新会话最后活跃时间
            conversationService.updateLastActiveTime(conversation.getId());
            
            // 6. 构建响应 - 需要将Message实体转换为MessageDTO
            MessageDTO messageDTO = convertToMessageDTO(message);

            // 设置消息的发送者信息
            try {
                // 获取发送者的用户信息
                com.im.imcommunicationsystem.user.dto.response.UserProfileResponse senderProfile = 
                    userProfileService.getUserProfile(senderId);
                
                if (senderProfile != null) {
                    messageDTO.setSenderNickname(senderProfile.getNickname());
                    messageDTO.setSenderAvatar(senderProfile.getAvatarUrl());
                }
                
                // 设置isSentByCurrentUser字段为true，因为这是当前用户发送的消息
                messageDTO.setIsSentByCurrentUser(true);
            } catch (Exception e) {
                log.warn("Failed to get sender profile for user {}: {}", senderId, e.getMessage());
            }

            MessageResponse response = MessageResponse.success(messageDTO);
            response.setResponseMessage("消息发送成功");
            
            log.info("Successfully sent message {} from user {} to conversation {}", message.getId(), senderId, conversation.getId());
            
            // TODO: 7. 发送WebSocket通知
            // TODO: 8. 索引到搜索引擎
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to send message from user {} to conversation {}: {}", senderId, request.getConversationId(), e.getMessage(), e);
            throw new RuntimeException("发送消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将Message实体转换为MessageDTO
     * 
     * @param message 消息实体
     * @return 消息DTO
     */
    private MessageDTO convertToMessageDTO(Message message) {
        if (message == null) {
            return null;
        }
        
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .mediaFileId(message.getMediaFileId())
                .replyToMessageId(message.getReplyToMessageId())
                .forwardFromMessageId(message.getOriginalMessageId())
                .status(message.getStatus())
                .edited(message.getEdited())
                .editedAt(message.getEditedAt())
                .metadata(message.getMetadata())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    @Override
    public void recallMessage(Long messageId, RecallMessageRequest request, Long userId) {
        // TODO: 实现撤回消息逻辑
        // 1. 验证消息权限
        // 2. 检查撤回时间限制
        // 3. 更新消息状态
        // 4. 发送WebSocket通知
        // 5. 更新搜索索引
        log.info("Recalling message {} by user {}", messageId, userId);
    }

    @Override
    public MessageResponse editMessage(Long messageId, EditMessageRequest request, Long userId) {
        // TODO: 实现编辑消息逻辑
        // 1. 验证消息权限
        // 2. 检查编辑时间限制
        // 3. 更新消息内容
        // 4. 记录编辑历史
        // 5. 发送WebSocket通知
        // 6. 更新搜索索引
        log.info("Editing message {} by user {}", messageId, userId);
        return null;
    }

    @Override
    public void forwardMessage(ForwardMessageRequest request, Long userId) {
        // TODO: 实现转发消息逻辑
        // 1. 验证原消息权限
        // 2. 验证目标会话权限
        // 3. 创建转发消息
        // 4. 发送WebSocket通知
        // 5. 索引到搜索引擎
        log.info("Forwarding messages by user {}", userId);
    }

    @Override
    public void markAsRead(MarkAsReadRequest request, Long userId) {
        // TODO: 实现标记已读逻辑
        // 1. 验证会话权限
        // 2. 更新已读状态
        // 3. 发送WebSocket通知
        log.info("Marking messages as read by user {} in conversation {}", userId, request.getConversationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Long conversationId, Long userId, Pageable pageable) {
        log.info("Getting messages for conversation {} by user {}, pageable: {}", conversationId, userId, pageable);
        
        try {
            // 1. 验证参数
            if (conversationId == null || userId == null) {
                log.error("Invalid parameters: conversationId={}, userId={}", conversationId, userId);
                throw new IllegalArgumentException("会话ID和用户ID不能为空");
            }
            
            // 2. 验证用户是否有权限访问该会话
            log.debug("Checking if user {} has access to conversation {}", userId, conversationId);
            boolean hasAccess = conversationService.isUserInConversation(conversationId, userId);
            log.debug("User {} access to conversation {}: {}", userId, conversationId, hasAccess);
            
            if (!hasAccess) {
                log.warn("User {} does not have permission to access conversation {}", userId, conversationId);
                throw new RuntimeException("您没有权限访问此会话");
            }
            
            // 3. 查询会话中的消息（排除已删除的消息）
            log.debug("Querying messages for conversation {} with pageable {}", conversationId, pageable);
            Page<Message> messages = messageRepository.findByConversationIdAndStatusNotOrderByCreatedAtDesc(
                    conversationId, MessageStatus.DELETED, pageable);
            log.debug("Found {} messages for conversation {}", messages.getTotalElements(), conversationId);
            
            // 4. 转换为MessageResponse
            List<MessageResponse> messageResponses = messages.getContent().stream()
                    .map(message -> {
                        MessageResponse response = convertToMessageResponse(message);
                        
                        // 设置isSentByCurrentUser字段，表示消息是否由当前用户发送
                        if (response != null && response.getMessage() != null) {
                            boolean isSentByCurrentUser = message.getSenderId().equals(userId);
                            response.getMessage().setIsSentByCurrentUser(isSentByCurrentUser);
                        }
                        
                        return response;
                    })
                    .collect(Collectors.toList());
            log.debug("Converted {} messages to response objects", messageResponses.size());
            
            Page<MessageResponse> result = new PageImpl<>(messageResponses, pageable, messages.getTotalElements());
            log.info("Successfully retrieved {} messages for conversation {} by user {}", result.getTotalElements(), conversationId, userId);
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to get messages for conversation {} by user {}: {}", conversationId, userId, e.getMessage(), e);
            throw new RuntimeException("获取消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 将Message实体转换为MessageResponse
     * 
     * @param message 消息实体
     * @return 消息响应
     */
    private MessageResponse convertToMessageResponse(Message message) {
        if (message == null) {
            return null;
        }
        
        MessageDTO messageDTO = convertToMessageDTO(message);
        
        // 尝试获取发送者信息
        try {
            // 获取发送者的用户信息
            com.im.imcommunicationsystem.user.dto.response.UserProfileResponse senderProfile = 
                userProfileService.getUserProfile(message.getSenderId());
            
            if (senderProfile != null) {
                messageDTO.setSenderNickname(senderProfile.getNickname());
                messageDTO.setSenderAvatar(senderProfile.getAvatarUrl());
            }
        } catch (Exception e) {
            log.warn("Failed to get sender profile for user {}: {}", message.getSenderId(), e.getMessage());
        }
        
        return MessageResponse.success(messageDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessageHistory(Long conversationId, Long beforeMessageId, Pageable pageable, Long userId) {
        // TODO: 实现获取消息历史逻辑
        // 1. 验证会话权限
        // 2. 查询消息历史
        // 3. 转换为响应对象
        log.info("Getting message history for conversation {} by user {}", conversationId, userId);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Long messageId) {
        // TODO: 实现根据ID获取消息逻辑
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesByIds(List<Long> messageIds) {
        // TODO: 实现批量获取消息逻辑
        return null;
    }

    @Override
    public void deleteMessage(Long messageId, Long userId) {
        // TODO: 实现删除消息逻辑
        // 1. 验证消息权限
        // 2. 软删除消息
        // 3. 发送WebSocket通知
        // 4. 删除搜索索引
        log.info("Deleting message {} by user {}", messageId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadMessageCount(Long conversationId, Long userId) {
        // TODO: 实现获取未读消息数量逻辑
        return 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getLastMessage(Long conversationId) {
        // TODO: 实现获取最后一条消息逻辑
        return null;
    }
}