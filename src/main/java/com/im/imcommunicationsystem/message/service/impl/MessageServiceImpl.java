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
import com.im.imcommunicationsystem.message.enums.ConversationType;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import com.im.imcommunicationsystem.group.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import com.im.imcommunicationsystem.user.service.UserProfileService;
import com.im.imcommunicationsystem.user.dto.response.UserProfileResponse;
import com.im.imcommunicationsystem.message.enums.MessageType;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import java.util.Map;
import java.util.HashMap;
import com.im.imcommunicationsystem.common.service.impl.WebSocketServiceImpl.WebSocketMessageEvent;
import org.springframework.context.event.EventListener;
import java.util.ArrayList;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import java.util.Optional;
import com.im.imcommunicationsystem.message.service.ReadStatusService;

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
    private final com.im.imcommunicationsystem.common.service.WebSocketService webSocketService;
    private final @Lazy ContactService contactService;
    private final @Lazy GroupMemberService groupMemberService;
    private final @Lazy ConversationMemberRepository conversationMemberRepository;
    // 添加 ReadStatusService 依赖
    private final ReadStatusService readStatusService;

    @Override
    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        log.info("Sending message from user {} to conversation {}", senderId, request.getConversationId());
        
        try {
            // 1. 验证请求参数
            if (!request.isValid()) {
                log.warn("Invalid send message request from user {}: {}", senderId, request.getValidationErrors());
                return MessageResponse.error("请求参数无效: " + String.join(", ", request.getValidationErrors()));
            }
            
            // 2. 获取或创建会话
            Conversation conversation;
            if (request.getConversationId() != null) {
                // 使用现有会话
                conversation = conversationService.getConversationById(request.getConversationId());
                if (conversation == null) {
                    log.warn("Conversation not found: {}", request.getConversationId());
                    return MessageResponse.error("会话不存在");
                }
                
                // 检查会话类型，如果是群聊，需要检查用户是否在群组中
                if (conversation.getConversationType() == ConversationType.GROUP) {
                    // 检查用户是否是群组成员
                    Long groupId = conversation.getRelatedGroupId();
                    if (groupId != null) {
                        try {
                            // 尝试获取群成员信息，如果不是群成员会抛出异常
                            GroupMember groupMember = groupMemberService.getGroupMember(groupId, senderId);
                            
                            // 检查用户是否被禁言
                            if (groupMember.isMuted()) {
                                log.warn("User {} is muted in group {}, cannot send message", senderId, groupId);
                                return MessageResponse.error("您已被禁言，无法发送消息");
                            }
                        } catch (Exception e) {
                            log.warn("User {} is not a member of group {}, cannot send message", senderId, groupId);
                            return MessageResponse.error("您不是群组成员，无法发送消息");
                        }
                    }
                }
                
                // 如果是私聊，检查双方是否为好友
                if (conversation.getConversationType() == ConversationType.PRIVATE) {
                    // 获取对方的用户ID
                    Long recipientId = null;
                    // 从成员列表中找出接收者ID（非发送者的那个ID）
                    List<Long> memberIds = conversationService.getConversationMemberIds(conversation.getId());
                    for (Long memberId : memberIds) {
                        if (!memberId.equals(senderId)) {
                            recipientId = memberId;
                            break;
                        }
                    }
                    
                    if (recipientId == null) {
                        log.warn("Cannot determine recipient for conversation {}", conversation.getId());
                        return MessageResponse.error("无法确定消息接收者");
                    }
                    
                    // 检查是否为好友关系
                    boolean isFriendResult = contactService.isFriend(senderId, recipientId);
                    log.info("好友关系检查结果: senderId={}, recipientId={}, isFriend={}", senderId, recipientId, isFriendResult);
                    
                    if (!isFriendResult) {
                        log.warn("User {} is not friend with user {}, cannot send message", senderId, recipientId);
                        return MessageResponse.error("您已不是对方好友，无法发送消息");
                    }
                    
                    // 检查是否被屏蔽
                    boolean isBlockedResult = contactService.isBlocked(recipientId, senderId);
                    log.info("屏蔽状态检查结果: recipientId={}, senderId={}, isBlocked={}", recipientId, senderId, isBlockedResult);
                    
                    if (isBlockedResult) {
                        log.warn("User {} is blocked by user {}, cannot send message", senderId, recipientId);
                        return MessageResponse.error("您已被对方屏蔽，无法发送消息");
                    }
                }
            } else if (request.getRecipientId() != null) {
                // 创建新的私聊会话
                Long recipientId = request.getRecipientId();
                
                // 检查是否为好友关系
                boolean isFriendResult = contactService.isFriend(senderId, recipientId);
                log.info("新会话好友关系检查结果: senderId={}, recipientId={}, isFriend={}", senderId, recipientId, isFriendResult);
                
                if (!isFriendResult) {
                    log.warn("User {} is not friend with user {}, cannot send message", senderId, recipientId);
                    return MessageResponse.error("您不是对方好友，无法发送消息");
                }
                
                // 检查是否被对方屏蔽
                boolean isBlockedResult = contactService.isBlocked(recipientId, senderId);
                log.info("新会话屏蔽状态检查结果: recipientId={}, senderId={}, isBlocked={}", recipientId, senderId, isBlockedResult);
                
                if (isBlockedResult) {
                    log.warn("User {} is blocked by user {}, cannot send message", senderId, recipientId);
                    return MessageResponse.error("您已被对方屏蔽，无法发送消息");
                }
                
                conversation = conversationService.getOrCreatePrivateConversation(senderId, recipientId);
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
            
            // 7. 通过WebSocket发送实时消息
                try {
                    // 创建WebSocket消息
                    Map<String, Object> wsMessage = new HashMap<>();
                    wsMessage.put("type", "MESSAGE");
                    wsMessage.put("data", messageDTO);
                    
                // 发送给会话的所有成员（包括接收者和发送者）
                // 修改: 不再排除发送者自己，确保发送者也能收到消息
                webSocketService.sendMessageToConversation(conversation.getId(), wsMessage, null);
                log.debug("WebSocket消息已发送给会话{}的所有成员（包括发送者）", conversation.getId());
                } catch (Exception e) {
                    log.error("发送WebSocket消息失败: {}", e.getMessage(), e);
                    // 不影响主流程，继续执行
                }
            
            // 8. 发送确认消息给发送者
            try {
                // 创建确认消息
                Map<String, Object> confirmationMessage = new HashMap<>();
                confirmationMessage.put("type", "MESSAGE_CONFIRMATION");
                confirmationMessage.put("data", Map.of(
                    "messageId", message.getId(),
                    "tempId", request.getTempId(), // 临时ID，用于前端消息确认
                    "conversationId", conversation.getId(),
                    "status", message.getStatus().toString(),
                    "timestamp", System.currentTimeMillis()
                ));
                
                // 发送给发送者
                webSocketService.sendMessageToUser(senderId, confirmationMessage);
                log.debug("确认消息已发送给用户{}", senderId);
            } catch (Exception e) {
                log.error("发送确认消息失败: {}", e.getMessage(), e);
                // 不影响主流程，继续执行
            }
            
            // TODO: 9. 索引到搜索引擎
            
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
                .isRead(message.getIsRead())
                .build();
    }

    @Override
    public void recallMessage(Long messageId, RecallMessageRequest request, Long userId) {
        log.info("Recalling message {} by user {}", messageId, userId);
        
        try {
            // 1. 获取消息
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("消息不存在"));
            
            // 2. 验证消息权限 - 只有发送者或管理员可以撤回消息
            if (!message.getSenderId().equals(userId) && !request.isAdminRecall()) {
                log.warn("User {} attempted to recall message {} but is not the sender", userId, messageId);
                throw new RuntimeException("您没有权限撤回此消息");
            }
            
            // 3. 检查撤回时间限制 - 默认2分钟内可撤回
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime messageTime = message.getCreatedAt();
            long minutesPassed = java.time.Duration.between(messageTime, now).toMinutes();
            
            // 如果不是管理员撤回，且超过了时间限制，则不允许撤回
            if (!request.isAdminRecall() && minutesPassed > 2) {
                log.warn("User {} attempted to recall message {} but exceeded time limit ({} minutes)", 
                        userId, messageId, minutesPassed);
                throw new RuntimeException("消息发送超过2分钟，无法撤回");
            }
            
            // 4. 更新消息状态为已撤回
            message.setStatus(MessageStatus.RECALLED);
            message.setUpdatedAt(now);
            
            // 5. 如果有撤回原因，保存到元数据中
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                // 简单处理元数据，实际应用中可能需要更复杂的JSON处理
                message.setMetadata(String.format("{\"recallReason\":\"%s\",\"recalledBy\":%d}", 
                        request.getReason(), userId));
            }
            
            // 6. 保存更新后的消息
            messageRepository.save(message);
            
            log.info("Successfully recalled message {} by user {}", messageId, userId);
            
            // 7. 通过WebSocket通知所有会话成员消息已被撤回
            try {
                // 创建通知数据
                Map<String, Object> recallData = new HashMap<>();
                recallData.put("messageId", messageId);
                recallData.put("conversationId", message.getConversationId());
                recallData.put("recalledBy", userId);
                recallData.put("recalledAt", now.toString());
                if (request.getReason() != null && !request.getReason().isEmpty()) {
                    recallData.put("reason", request.getReason());
                }
                
                // 创建WebSocket消息
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "RECALL");
                wsMessage.put("data", recallData);
                
                // 广播给会话的所有成员，包括发送者自己
                webSocketService.sendMessageToConversation(message.getConversationId(), wsMessage, null);
                log.info("WebSocket撤回通知已发送给会话{}的所有成员", message.getConversationId());
            } catch (Exception e) {
                log.error("发送WebSocket撤回通知失败: {}", e.getMessage(), e);
                // 不影响主流程继续执行
            }
            
            // 8. TODO: 更新搜索索引
            
        } catch (Exception e) {
            log.error("Failed to recall message {} by user {}: {}", messageId, userId, e.getMessage(), e);
            throw new RuntimeException("撤回消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageResponse editMessage(Long messageId, EditMessageRequest request, Long userId) {
        log.info("Editing message {} by user {}", messageId, userId);
        
        try {
            // 1. 获取消息
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("消息不存在"));
            
            // 2. 验证消息权限 - 只有发送者可以编辑消息
            if (!message.getSenderId().equals(userId)) {
                log.warn("User {} attempted to edit message {} but is not the sender", userId, messageId);
                throw new RuntimeException("您没有权限编辑此消息");
            }
            
            // 3. 检查编辑时间限制 - 默认5分钟内可编辑
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime messageTime = message.getCreatedAt();
            long minutesPassed = java.time.Duration.between(messageTime, now).toMinutes();
            
            if (minutesPassed > 5) {
                log.warn("User {} attempted to edit message {} but exceeded time limit ({} minutes)", 
                        userId, messageId, minutesPassed);
                throw new RuntimeException("消息发送超过5分钟，无法编辑");
            }
            
            // 4. 检查消息类型 - 只有文本消息可以编辑
            if (message.getMessageType() != MessageType.TEXT) {
                log.warn("User {} attempted to edit non-text message {}", userId, messageId);
                throw new RuntimeException("只有文本消息可以编辑");
            }
            
            // 5. 检查内容是否有实际变化
            if (!request.hasContentChanged(message.getContent())) {
                log.info("Message content has not changed, skipping update");
                return convertToMessageResponse(message);
            }
            
            // 6. 更新消息内容
            message.setContent(request.getContent());
            message.setEdited(true);
            message.setEditedAt(now);
            message.setUpdatedAt(now);
            
            // 7. 如果有编辑原因，保存到元数据中
            if (request.getEditReason() != null && !request.getEditReason().isEmpty()) {
                // 简单处理元数据，实际应用中可能需要更复杂的JSON处理
                message.setMetadata(String.format("{\"editReason\":\"%s\",\"editedBy\":%d}", 
                        request.getEditReason(), userId));
            }
            
            // 8. 保存更新后的消息
            message = messageRepository.save(message);
            
            log.info("Successfully edited message {} by user {}", messageId, userId);
            
            // 9. 通过WebSocket通知所有会话成员消息已被编辑
            try {
                // 创建通知数据
                Map<String, Object> editData = new HashMap<>();
                editData.put("messageId", messageId);
                editData.put("conversationId", message.getConversationId());
                editData.put("content", message.getContent());
                editData.put("editedBy", userId);
                editData.put("editedAt", now.toString());
                if (request.getEditReason() != null && !request.getEditReason().isEmpty()) {
                    editData.put("editReason", request.getEditReason());
                }
                
                // 创建WebSocket消息
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "EDIT");
                wsMessage.put("data", editData);
                
                // 广播给会话的所有成员，包括发送者自己
                webSocketService.sendMessageToConversation(message.getConversationId(), wsMessage, null);
                log.info("WebSocket编辑通知已发送给会话{}的所有成员", message.getConversationId());
            } catch (Exception e) {
                log.error("发送WebSocket编辑通知失败: {}", e.getMessage(), e);
                // 不影响主流程继续执行
            }
            
            // 10. TODO: 更新搜索索引
            
            // 11. 返回更新后的消息
            return convertToMessageResponse(message);
            
        } catch (Exception e) {
            log.error("Failed to edit message {} by user {}: {}", messageId, userId, e.getMessage(), e);
            throw new RuntimeException("编辑消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardMessage(ForwardMessageRequest request, Long userId) {
        log.info("Forwarding messages by user {}: messageIds={}, targetConversationIds={}, forwardType={}", 
                userId, request.getMessageIds(), request.getTargetConversationIds(), request.getForwardType());
        
        try {
            // 1. 验证请求
            if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
                log.warn("No message IDs provided in forward request by user {}", userId);
                throw new RuntimeException("请选择要转发的消息");
            }
            
            if (request.getTargetConversationIds() == null || request.getTargetConversationIds().isEmpty()) {
                log.warn("No target conversation IDs provided in forward request by user {}", userId);
                throw new RuntimeException("请选择转发目标");
            }
            
            // 2. 获取要转发的消息
            List<Message> messagesToForward = messageRepository.findAllById(request.getMessageIds());
            
            if (messagesToForward.isEmpty()) {
                log.warn("No messages found for IDs: {} by user {}", request.getMessageIds(), userId);
                throw new RuntimeException("未找到要转发的消息");
            }
            
            log.info("Found {} messages to forward", messagesToForward.size());
            
            // 3. 验证目标会话
            for (Long targetConversationId : request.getTargetConversationIds()) {
                Conversation targetConversation = conversationService.getConversationById(targetConversationId);
                
                if (targetConversation == null || targetConversation.isDeleted()) {
                    log.warn("Target conversation {} not found or deleted", targetConversationId);
                    throw new RuntimeException("目标会话不存在或已删除");
                }
                
                // 验证用户是否有权限在此会话中发送消息
                if (!conversationService.isUserInConversation(targetConversation.getId(), userId)) {
                    log.warn("User {} does not have permission to send message in conversation {}", userId, targetConversation.getId());
                    throw new RuntimeException("您没有权限在目标会话中发送消息");
                }
            }
            
            // 4. 根据转发类型处理
            if (request.isMergeForward()) {
                // 合并转发 - 将多条消息合并为一条
                handleMergeForward(messagesToForward, request.getTargetConversationIds(), userId, request.getComment());
            } else {
                // 逐条转发 - 每条消息单独转发
                handleSeparateForward(messagesToForward, request.getTargetConversationIds(), userId, request.getComment());
            }
            
            log.info("Successfully forwarded messages by user {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to forward messages by user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("转发消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理合并转发
     * 
     * @param messagesToForward 要转发的消息列表
     * @param targetConversationIds 目标会话ID列表
     * @param userId 当前用户ID
     * @param comment 附加评论
     */
    private void handleMergeForward(List<Message> messagesToForward, List<Long> targetConversationIds, Long userId, String comment) {
        log.info("Handling merge forward for {} messages to {} conversations", messagesToForward.size(), targetConversationIds.size());
        
        // 构建合并消息内容
        StringBuilder mergedContent = new StringBuilder();
        
        // 如果有附加评论，先添加评论
        if (comment != null && !comment.trim().isEmpty()) {
            mergedContent.append(comment).append("\n\n");
        }
        
        mergedContent.append("--- 合并转发 ---\n\n");
        
        // 添加每条消息的内容
        for (int i = 0; i < messagesToForward.size(); i++) {
            Message message = messagesToForward.get(i);
            
            // 获取发送者信息
            String senderName = "用户" + message.getSenderId();
            try {
                UserProfileResponse senderProfile = userProfileService.getUserProfile(message.getSenderId());
                if (senderProfile != null && senderProfile.getNickname() != null) {
                    senderName = senderProfile.getNickname();
                }
            } catch (Exception e) {
                log.warn("Failed to get sender profile for user {}: {}", message.getSenderId(), e.getMessage());
            }
            
            // 添加发送者和消息内容
            mergedContent.append(senderName).append(": ");
            
            // 根据消息类型添加内容
            if (message.getMessageType() == MessageType.TEXT) {
                mergedContent.append(message.getContent());
            } else if (message.getMessageType() == MessageType.IMAGE) {
                mergedContent.append("[图片]");
            } else if (message.getMessageType() == MessageType.VIDEO) {
                mergedContent.append("[视频]");
            } else if (message.getMessageType() == MessageType.FILE) {
                mergedContent.append("[文件]");
            } else if (message.getMessageType() == MessageType.AUDIO) {
                mergedContent.append("[语音]");
            } else {
                mergedContent.append("[").append(message.getMessageType()).append("]");
            }
            
            // 如果不是最后一条消息，添加换行符
            if (i < messagesToForward.size() - 1) {
                mergedContent.append("\n");
            }
        }
        
        // 为每个目标会话发送合并后的消息
        for (Long targetConversationId : targetConversationIds) {
            // 创建发送消息请求
            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .conversationId(targetConversationId)
                    .messageType(MessageType.TEXT)
                    .content(mergedContent.toString())
                    .build();
            
            // 发送消息
            sendMessage(sendRequest, userId);
        }
    }
    
    /**
     * 处理逐条转发
     * 
     * @param messagesToForward 要转发的消息列表
     * @param targetConversationIds 目标会话ID列表
     * @param userId 当前用户ID
     * @param comment 附加评论
     */
    private void handleSeparateForward(List<Message> messagesToForward, List<Long> targetConversationIds, Long userId, String comment) {
        log.info("Handling separate forward for {} messages to {} conversations", messagesToForward.size(), targetConversationIds.size());
        
        // 如果有附加评论，先发送评论
        if (comment != null && !comment.trim().isEmpty()) {
            for (Long targetConversationId : targetConversationIds) {
                SendMessageRequest commentRequest = SendMessageRequest.builder()
                        .conversationId(targetConversationId)
                        .messageType(MessageType.TEXT)
                        .content(comment)
                        .build();
                
                sendMessage(commentRequest, userId);
            }
        }
        
        // 逐条转发每条消息到每个目标会话
        for (Message message : messagesToForward) {
            for (Long targetConversationId : targetConversationIds) {
                // 创建发送消息请求
                SendMessageRequest sendRequest = SendMessageRequest.builder()
                        .conversationId(targetConversationId)
                        .messageType(message.getMessageType())
                        .content(message.getContent())
                        .mediaFileId(message.getMediaFileId())
                        .forwardFromMessageId(message.getId())  // 设置原始消息ID
                        .build();
                
                // 发送消息
                sendMessage(sendRequest, userId);
            }
        }
    }

    @Override
    public void markAsRead(MarkAsReadRequest request, Long userId) {
        log.info("Marking messages as read by user {} in conversation {}", userId, request.getConversationId());
        
        try {
            // 1. 验证参数
            if (request.getConversationId() == null || userId == null) {
                log.error("Invalid parameters: conversationId={}, userId={}", request.getConversationId(), userId);
                throw new IllegalArgumentException("会话ID和用户ID不能为空");
            }
            
            // 2. 验证用户是否有权限访问该会话
            boolean hasAccess = conversationService.isUserInConversation(request.getConversationId(), userId);
            if (!hasAccess) {
                log.warn("User {} does not have permission to access conversation {}", userId, request.getConversationId());
                throw new RuntimeException("您没有权限访问此会话");
            }
            
            // 3. 根据请求类型执行不同的标记已读逻辑
            if (request.isMarkAllAsRead()) {
                // 标记会话中所有消息为已读
                log.info("Marking all messages as read in conversation {}", request.getConversationId());
                markAllMessagesAsRead(request.getConversationId(), userId);
            } else if (request.isBatchMarkToMessage()) {
                // 标记到指定消息为止的所有消息为已读
                log.info("Marking messages up to {} as read in conversation {}", 
                        request.getLastReadMessageId(), request.getConversationId());
                markMessagesToIdAsRead(request.getConversationId(), request.getLastReadMessageId(), userId);
            } else if (request.isSpecificMessages()) {
                // 标记指定的消息列表为已读
                log.info("Marking specific messages as read: {} in conversation {}", 
                        request.getMessageIds(), request.getConversationId());
                markSpecificMessagesAsRead(request.getConversationId(), request.getMessageIds(), userId);
            } else {
                // 默认标记所有未读消息为已读
                log.info("No specific mark as read type specified, defaulting to mark all as read in conversation {}", request.getConversationId());
                markAllMessagesAsRead(request.getConversationId(), userId);
            }
            
            log.info("Successfully marked messages as read for user {} in conversation {}", 
                    userId, request.getConversationId());
            
        } catch (Exception e) {
            log.error("Failed to mark messages as read: {}", e.getMessage(), e);
            throw new RuntimeException("标记消息已读失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 标记会话中所有消息为已读
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    private void markAllMessagesAsRead(Long conversationId, Long userId) {
        // 查找所有非当前用户发送的未读消息
        List<Message> unreadMessages = messageRepository.findByConversationIdAndSenderIdNotAndIsReadFalse(
                conversationId, userId);
        
        log.info("Found {} unread messages in conversation {} for user {}", 
                unreadMessages.size(), conversationId, userId);
        
        if (unreadMessages.isEmpty()) {
            log.info("No unread messages found in conversation {}", conversationId);
            return;
        }
        
        // 更新消息为已读
        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setUpdatedAt(LocalDateTime.now());
            log.debug("Marking message {} as read", message.getId());
        }
        
        // 批量保存更新
        messageRepository.saveAll(unreadMessages);
        log.info("Marked {} messages as read in conversation {}", unreadMessages.size(), conversationId);
        
        // 记录每条消息的ID，方便调试
        if (log.isDebugEnabled()) {
            List<Long> messageIds = unreadMessages.stream()
                    .map(Message::getId)
                    .collect(Collectors.toList());
            log.debug("Marked message IDs: {}", messageIds);
        }
    }
    
    /**
     * 标记到指定消息ID为止的所有消息为已读
     * 
     * @param conversationId 会话ID
     * @param lastReadMessageId 最后已读消息ID
     * @param userId 用户ID
     */
    private void markMessagesToIdAsRead(Long conversationId, Long lastReadMessageId, Long userId) {
        // 获取指定消息
        Message lastReadMessage = messageRepository.findById(lastReadMessageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        // 验证消息是否属于指定会话
        if (!lastReadMessage.getConversationId().equals(conversationId)) {
            throw new RuntimeException("消息不属于指定会话");
        }
        
        // 查找所有在此消息之前且非当前用户发送的未读消息
        List<Message> messagesToMark = messageRepository.findByConversationIdAndSenderIdNotAndCreatedAtLessThanEqualAndIsReadFalse(
                conversationId, userId, lastReadMessage.getCreatedAt());
        
        if (messagesToMark.isEmpty()) {
            log.debug("No messages to mark as read up to message {} in conversation {}", lastReadMessageId, conversationId);
            return;
        }
        
        // 更新消息为已读
        for (Message message : messagesToMark) {
            message.setIsRead(true);
            message.setUpdatedAt(LocalDateTime.now());
        }
        
        // 批量保存更新
        messageRepository.saveAll(messagesToMark);
        log.debug("Marked {} messages as read up to message {} in conversation {}", 
                messagesToMark.size(), lastReadMessageId, conversationId);
    }
    
    /**
     * 标记指定消息列表为已读
     * 
     * @param conversationId 会话ID
     * @param messageIds 消息ID列表
     * @param userId 用户ID
     */
    private void markSpecificMessagesAsRead(Long conversationId, List<Long> messageIds, Long userId) {
        // 查找指定的消息
        List<Message> messagesToMark = messageRepository.findByIdInAndConversationId(messageIds, conversationId);
        
        if (messagesToMark.isEmpty()) {
            log.debug("No messages found with IDs {} in conversation {}", messageIds, conversationId);
            return;
        }
        
        // 过滤出非当前用户发送的未读消息
        List<Message> unreadMessages = messagesToMark.stream()
                .filter(message -> !message.getSenderId().equals(userId) && !message.getIsRead())
                .collect(Collectors.toList());
        
        if (unreadMessages.isEmpty()) {
            log.debug("No unread messages found among specified IDs in conversation {}", conversationId);
            return;
        }
        
        // 更新消息为已读
        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setUpdatedAt(LocalDateTime.now());
        }
        
        // 批量保存更新
        messageRepository.saveAll(unreadMessages);
        log.debug("Marked {} specific messages as read in conversation {}", unreadMessages.size(), conversationId);
    }

    @Override
    @Transactional
    public Page<MessageResponse> getConversationMessages(Long conversationId, Long userId, Pageable pageable) {
        log.info("获取会话{}的消息，用户={}, 分页={}", conversationId, userId, pageable);
        
        try {
            // 验证会话存在并且用户有访问权限
            Conversation conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                log.warn("会话{}不存在", conversationId);
                throw new RuntimeException("会话不存在");
            }
            
            // 检查用户是否在该会话中
            if (!conversationService.isUserInConversation(conversationId, userId)) {
                log.warn("用户{}不在会话{}中", userId, conversationId);
                throw new RuntimeException("您不在此会话中，无法访问消息");
            }
            
            // 获取用户的会话成员信息，检查是否有lastAcceptableMessageId限制
            Optional<ConversationMember> memberOpt = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId);
            Long lastAcceptableMessageId = null;
            if (memberOpt.isPresent() && memberOpt.get().getLastAcceptableMessageId() != null) {
                lastAcceptableMessageId = memberOpt.get().getLastAcceptableMessageId();
                log.info("用户{}在会话{}中的最后可接受消息ID为{}", userId, conversationId, lastAcceptableMessageId);
            }
            
            // 查询会话消息
            Page<Message> messagePage;
            if (lastAcceptableMessageId != null) {
                // 如果有lastAcceptableMessageId限制，只查询ID小于等于该值的消息
                messagePage = messageRepository.findByConversationIdAndIdLessThanEqualAndStatusNotOrderByCreatedAtDesc(
                        conversationId, lastAcceptableMessageId, MessageStatus.DELETED, pageable);
                log.info("应用了lastAcceptableMessageId={}过滤，查询到{}条消息", lastAcceptableMessageId, messagePage.getContent().size());
            } else {
                // 否则查询所有非删除状态的消息
                messagePage = messageRepository.findByConversationIdAndStatusNotOrderByCreatedAtDesc(
                        conversationId, MessageStatus.DELETED, pageable);
            }
            
            // 记录查询结果信息
            log.info("会话{}消息查询结果: 总条数={}, 页码={}, 每页条数={}, 总页数={}", 
                    conversationId, 
                    messagePage.getTotalElements(),
                    messagePage.getNumber(),
                    messagePage.getSize(),
                    messagePage.getTotalPages());
            
            // 如果没有找到消息，记录警告日志
            if (messagePage.isEmpty()) {
                log.warn("会话{}没有消息记录", conversationId);
                // 返回空页面
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            // 记录部分消息详情以便调试
            if (!messagePage.isEmpty()) {
                Message firstMessage = messagePage.getContent().get(0);
                log.info("首条消息详情: id={}, senderId={}, content={}..., type={}, createdAt={}", 
                        firstMessage.getId(),
                        firstMessage.getSenderId(),
                        firstMessage.getContent().substring(0, Math.min(50, firstMessage.getContent().length())),
                        firstMessage.getMessageType(),
                        firstMessage.getCreatedAt());
                }
                
            // 将消息转换为DTO并返回
            Page<MessageResponse> responsePage = messagePage.map(message -> {
                MessageDTO messageDTO = convertToMessageDTO(message);
                
                // 确保DTO包含必要的字段
                if (messageDTO != null) {
                    // 添加发送者信息
                    try {
                        // 获取发送者的用户信息
                        com.im.imcommunicationsystem.user.dto.response.UserProfileResponse senderProfile = 
                            userProfileService.getUserProfile(message.getSenderId());
                        
                        if (senderProfile != null) {
                            messageDTO.setSenderNickname(senderProfile.getNickname());
                            messageDTO.setSenderAvatar(senderProfile.getAvatarUrl());
                        }
                        
                        // 设置消息是否由当前用户发送
                        messageDTO.setIsSentByCurrentUser(message.getSenderId().equals(userId));
                    } catch (Exception e) {
                        log.warn("获取用户{}资料失败: {}", message.getSenderId(), e.getMessage());
                        }
                }
                
                // 返回包含单个消息的响应
                return MessageResponse.success(messageDTO);
            });
            
            // 记录最终结果
            log.info("成功转换会话{}的消息，共{}条", conversationId, responsePage.getContent().size());
            
            // 标记消息为已读
            try {
                markAllMessagesAsRead(conversationId, userId);
            } catch (Exception e) {
                log.warn("标记会话{}消息为已读失败: {}", conversationId, e.getMessage());
                // 不影响主要返回
            }
            
            return responsePage;
        } catch (Exception e) {
            log.error("获取会话{}消息失败: {}", conversationId, e.getMessage(), e);
            throw new RuntimeException("获取消息失败: " + e.getMessage(), e);
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
        
        // 记录mediaFileId的日志
        if (message.getMediaFileId() != null) {
            log.info("消息 {} 包含媒体文件ID: {}", message.getId(), message.getMediaFileId());
            // 确保mediaFileId被正确设置
            messageDTO.setMediaFileId(message.getMediaFileId());
        }
        
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
        log.debug("Getting unread message count for conversation {} by user {}", conversationId, userId);
        
        try {
            // 使用 ReadStatusService 获取未读消息数量，确保不计算自己发送的消息
            return readStatusService.countUnreadMessages(userId, conversationId);
        } catch (Exception e) {
            log.error("Failed to get unread message count: {}", e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getLastMessage(Long conversationId) {
        // TODO: 实现获取最后一条消息逻辑
        return null;
    }

/**
 * 监听WebSocket消息事件
 */
@EventListener
public void handleWebSocketMessageEvent(WebSocketMessageEvent event) {
    log.info("接收到WebSocket消息事件: 会话ID={}, 发送者ID={}", event.getConversationId(), event.getSenderId());
    
    try {
        // 构建消息请求对象
        SendMessageRequest messageRequest = new SendMessageRequest();
        messageRequest.setConversationId(event.getConversationId());
        
        // 转换消息类型
        MessageType messageType = MessageType.TEXT; // 默认为TEXT
        try {
            messageType = MessageType.valueOf(event.getMessageType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无法解析消息类型: {}, 使用默认类型TEXT", event.getMessageType());
        }
        
        messageRequest.setMessageType(messageType);
        messageRequest.setContent(event.getContent());
        messageRequest.setMediaFileId(event.getMediaFileId());
        messageRequest.setTempMessageId(event.getTempId());
        
        // 保存消息到数据库
        log.info("保存WebSocket消息到数据库: 会话ID={}, 发送者ID={}, 内容={}", 
                event.getConversationId(), event.getSenderId(), event.getContent());
        
        var response = sendMessage(messageRequest, event.getSenderId());
        
        // 创建包含消息ID的确认消息
        if (response.getSuccess() != null && response.getSuccess() && response.getMessage() != null) {
            // 构建消息确认
            Map<String, Object> confirmationData = new HashMap<>();
            confirmationData.put("tempId", event.getTempId());
            confirmationData.put("id", response.getMessage().getId());
            confirmationData.put("messageId", response.getMessage().getId());
            confirmationData.put("status", "SENT");
            confirmationData.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> confirmationMessage = new HashMap<>();
            confirmationMessage.put("type", "MESSAGE_CONFIRMATION");
            confirmationMessage.put("data", confirmationData);
            
            // 通过WebSocket发送确认
            webSocketService.sendMessageToUser(event.getSenderId(), confirmationMessage);
        } else {
            // 发送错误响应
            String errorMessage = response.getResponseMessage() != null 
                    ? response.getResponseMessage() : "未知错误";
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("tempId", event.getTempId());
            errorData.put("error", "保存消息失败");
            errorData.put("message", errorMessage);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("data", errorData);
            
            // 发送错误响应给发送者
            webSocketService.sendMessageToUser(event.getSenderId(), errorResponse);
            
            log.error("保存WebSocket消息失败: {}", errorMessage);
            return;
        }
        
        // 注释掉这段代码，避免消息重复广播
        /* 
        // 如果消息保存成功，广播给会话成员
        if (response.getMessage() != null) {
            // 获取完整消息
            MessageDTO message = response.getMessage();
            
            // 构建要广播的消息
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("type", "MESSAGE");
            broadcastMessage.put("data", message);
            
            // 广播消息给会话的其他成员
            webSocketService.sendMessageToConversation(
                    event.getConversationId(), 
                    broadcastMessage, 
                    event.getSenderId() // 排除发送者自己
            );
            
            log.info("已广播消息给会话{}的成员", event.getConversationId());
        }
        */
        
    } catch (Exception e) {
        log.error("处理WebSocket消息事件时出错: {}", e.getMessage(), e);
    }
    }
}