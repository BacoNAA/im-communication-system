package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.message.entity.ReadStatus;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.message.repository.ReadStatusRepository;
import com.im.imcommunicationsystem.message.service.ReadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 已读状态服务实现类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-07-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadStatusServiceImpl implements ReadStatusService {

    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;

    @Override
    public ReadStatus updateReadStatus(Long userId, Long conversationId, Long lastReadMessageId) {
        log.info("Updating read status for user {} in conversation {}, lastReadMessageId: {}", 
                userId, conversationId, lastReadMessageId);
        
        try {
            // 查找现有的已读状态记录
            Optional<ReadStatus> existingStatus = readStatusRepository.findByUserIdAndConversationId(userId, conversationId);
            
            if (existingStatus.isPresent()) {
                ReadStatus status = existingStatus.get();
                
                // 只有当新的最后已读消息ID大于当前记录的ID时才更新
                if (lastReadMessageId > status.getLastReadMessageId()) {
                    log.debug("Updating existing read status: {} -> {}", 
                            status.getLastReadMessageId(), lastReadMessageId);
                    
                    status.setLastReadMessageId(lastReadMessageId);
                    status.setUpdatedAt(LocalDateTime.now());
                    return readStatusRepository.save(status);
                } else {
                    log.debug("Skipping update as new lastReadMessageId {} is not greater than current {}", 
                            lastReadMessageId, status.getLastReadMessageId());
                    return status;
                }
            } else {
                // 创建新的已读状态记录
                log.debug("Creating new read status record");
                ReadStatus newStatus = ReadStatus.builder()
                        .userId(userId)
                        .conversationId(conversationId)
                        .lastReadMessageId(lastReadMessageId)
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                return readStatusRepository.save(newStatus);
            }
        } catch (Exception e) {
            log.error("Failed to update read status: {}", e.getMessage(), e);
            throw new RuntimeException("更新已读状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReadStatus getReadStatus(Long userId, Long conversationId) {
        log.debug("Getting read status for user {} in conversation {}", userId, conversationId);
        
        try {
            return readStatusRepository.findByUserIdAndConversationId(userId, conversationId).orElse(null);
        } catch (Exception e) {
            log.error("Failed to get read status: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadStatus> getReadStatusByConversation(Long conversationId) {
        log.debug("Getting all read statuses for conversation {}", conversationId);
        
        try {
            return readStatusRepository.findByConversationId(conversationId);
        } catch (Exception e) {
            log.error("Failed to get read statuses by conversation: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadStatus> getReadStatusByUser(Long userId) {
        log.debug("Getting all read statuses for user {}", userId);
        
        try {
            return readStatusRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to get read statuses by user: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUnreadMessages(Long userId, Long conversationId) {
        log.debug("Counting unread messages for user {} in conversation {}", userId, conversationId);
        
        try {
            // 获取最后可接受的消息ID（会话中最新的消息ID）
            Long lastAcceptableMessageId = messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                    .map(message -> message.getId())
                    .orElse(Long.MAX_VALUE);
            
            // 获取用户在会话中的已读状态
            Optional<ReadStatus> readStatus = readStatusRepository.findByUserIdAndConversationId(userId, conversationId);
            
            // 如果没有已读状态记录，则所有消息都未读，但仍需限制在最后可接受的消息ID范围内
            if (readStatus.isEmpty()) {
                // 统计会话中不是由该用户发送且ID小于等于最后可接受ID的消息数量
                return messageRepository.countByConversationIdAndIdLessThanEqualAndSenderIdNot(
                        conversationId, lastAcceptableMessageId, userId);
            }
            
            // 获取最后已读消息ID
            Long lastReadMessageId = readStatus.get().getLastReadMessageId();
            
            // 记录详细日志，帮助诊断问题
            log.info("Count unread messages - lastReadMessageId: {}, lastAcceptableMessageId: {}, same: {}", 
                     lastReadMessageId, lastAcceptableMessageId, lastReadMessageId.equals(lastAcceptableMessageId));
            
            // 如果最后已读消息ID等于或大于最后可接受消息ID，则没有未读消息
            if (lastReadMessageId >= lastAcceptableMessageId) {
                return 0L;
            }
            
            // 统计会话中ID大于最后已读消息ID且小于等于最后可接受ID且不是由该用户发送的消息数量
            return messageRepository.countByConversationIdAndIdBetweenAndSenderIdNot(
                    conversationId, lastReadMessageId, lastAcceptableMessageId, userId);
            
        } catch (Exception e) {
            log.error("Failed to count unread messages: {}", e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public ReadStatus markAllAsRead(Long userId, Long conversationId) {
        log.info("Marking all messages as read for user {} in conversation {}", userId, conversationId);
        
        try {
            // 获取会话中的最新消息ID
            Long latestMessageId = messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                    .map(message -> message.getId())
                    .orElse(0L);
            
            if (latestMessageId > 0) {
                log.debug("Latest message ID in conversation {}: {}", conversationId, latestMessageId);
                return updateReadStatus(userId, conversationId, latestMessageId);
            } else {
                log.debug("No messages found in conversation {}", conversationId);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to mark all as read: {}", e.getMessage(), e);
            throw new RuntimeException("标记全部已读失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteReadStatus(Long userId, Long conversationId) {
        log.info("Deleting read status for user {} in conversation {}", userId, conversationId);
        
        try {
            readStatusRepository.deleteByUserIdAndConversationId(userId, conversationId);
            log.debug("Successfully deleted read status");
        } catch (Exception e) {
            log.error("Failed to delete read status: {}", e.getMessage(), e);
            throw new RuntimeException("删除已读状态失败: " + e.getMessage(), e);
        }
    }
} 