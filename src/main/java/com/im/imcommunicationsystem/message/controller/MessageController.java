package com.im.imcommunicationsystem.message.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.MessageResponse;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.service.ConversationService;
import com.im.imcommunicationsystem.message.service.MessageService;
import com.im.imcommunicationsystem.message.service.ReadStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息控制器
 * 处理消息发送、接收、操作相关的HTTP请求
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;
    private final ConversationService conversationService;
    private final com.im.imcommunicationsystem.message.repository.MessageRepository messageRepository;
    private final ReadStatusService readStatusService;

    /**
     * 发送消息
     * 
     * @param request 发送消息请求
     * @param authentication 认证信息
     * @return 消息响应
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long senderId = getUserIdFromAuthentication(authentication);
            if (senderId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("User {} sending message to conversation {} or recipient {}", 
                    senderId, request.getConversationId(), request.getRecipientId());
            
            // 调用服务层发送消息
            MessageResponse response = messageService.sendMessage(request, senderId);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest(response.getResponseMessage()));
            }
            
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("发送消息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从认证信息中获取用户ID
     * 
     * @param authentication 认证信息
     * @return 用户ID
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // 使用SecurityUtils来获取当前用户ID
        return securityUtils.getCurrentUserId();
    }

    /**
     * 撤回消息
     * 
     * @param messageId 消息ID
     * @param request 撤回消息请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/{messageId}/recall")
    public ResponseEntity<ApiResponse<Void>> recallMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody RecallMessageRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("User {} recalling message {}", userId, messageId);
            
            // 设置消息ID (确保请求中的消息ID与路径中的一致)
            request.setMessageId(messageId);
            
            // 调用服务层撤回消息
            messageService.recallMessage(messageId, request, userId);
            
        return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("Failed to recall message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("撤回消息失败: " + e.getMessage()));
        }
    }

    /**
     * 编辑消息
     * 
     * @param messageId 消息ID
     * @param request 编辑消息请求
     * @param authentication 认证信息
     * @return 消息响应
     */
    @PutMapping("/{messageId}/edit")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("User {} editing message {}", userId, messageId);
            
            // 设置消息ID (确保请求中的消息ID与路径中的一致)
            request.setMessageId(messageId);
            
            // 调用服务层编辑消息
            MessageResponse response = messageService.editMessage(messageId, request, userId);
            
            if (response != null) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("编辑消息失败"));
            }
        } catch (Exception e) {
            log.error("Failed to edit message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("编辑消息失败: " + e.getMessage()));
        }
    }

    /**
     * 转发消息
     * 
     * @param request 转发消息请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PostMapping("/forward")
    public ResponseEntity<ApiResponse<Void>> forwardMessage(
            @Valid @RequestBody ForwardMessageRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("User {} forwarding messages: {}", userId, request);
            
            // 调用服务层转发消息
            messageService.forwardMessage(request, userId);
            
        return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("Failed to forward messages: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("转发消息失败: " + e.getMessage()));
        }
    }

    /**
     * 批量标记消息为已读
     * 
     * @param request 标记已读请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Valid @RequestBody MarkAsReadRequest request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("User {} marking messages as read in conversation {}, request: {}", 
                    userId, request.getConversationId(), request);
            
            // 使用新的ReadStatusService标记消息为已读
            if (request.isMarkAllAsRead()) {
                // 标记所有消息为已读
                readStatusService.markAllAsRead(userId, request.getConversationId());
            } else if (request.getLastReadMessageId() != null) {
                // 标记指定消息ID之前的所有消息为已读
                readStatusService.updateReadStatus(userId, request.getConversationId(), request.getLastReadMessageId());
            } else if (request.getMessageIds() != null && !request.getMessageIds().isEmpty()) {
                // 标记指定消息为已读
                // 找出最大的消息ID
                Long maxMessageId = request.getMessageIds().stream()
                        .max(Long::compareTo)
                        .orElse(0L);
                
                // 更新已读状态
                readStatusService.updateReadStatus(userId, request.getConversationId(), maxMessageId);
            } else {
                log.warn("Invalid mark as read request: {}", request);
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("无效的请求参数"));
            }
            
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("标记消息已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记单条消息为已读
     * 
     * @param messageId 消息ID
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Marking message {} as read by user {}", messageId, userId);
            
            // 获取消息
            Message message = messageService.getMessageById(messageId);
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查用户是否有权限标记该消息
            boolean hasAccess = conversationService.isUserInConversation(message.getConversationId(), userId);
            if (!hasAccess) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("无权标记此消息"));
            }
            
            // 如果消息是由当前用户发送的，则不需要标记为已读
            if (message.getSenderId().equals(userId)) {
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            
            // 标记消息为已读
            message.setIsRead(true);
            message.setUpdatedAt(LocalDateTime.now());
            messageRepository.save(message);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("标记消息已读失败: " + e.getMessage()));
        }
    }

    /**
     * 获取会话消息
     * 
     * @param conversationId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @param authentication 认证信息
     * @return 消息分页结果
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Getting messages for conversation {}, page={}, size={}, userId={}", 
                    conversationId, page, size, userId);
            
            // 创建分页请求
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // 获取消息
            Page<MessageResponse> messages = messageService.getConversationMessages(conversationId, userId, pageable);
            
            // 输出详细日志
            log.info("Retrieved {} messages for conversation {}", 
                    messages.getContent().size(), conversationId);
            
            // 如果消息列表为空，输出警告
            if (messages.isEmpty()) {
                log.warn("No messages found for conversation {}", conversationId);
            } else {
                // 记录第一条消息的关键信息，帮助调试
                MessageResponse firstMsg = messages.getContent().get(0);
                log.info("First message details - ID: {}, senderId: {}, content: {}, messageType: {}", 
                        firstMsg.getMessage().getId(),
                        firstMsg.getMessage().getSenderId(),
                        firstMsg.getMessage().getContent().substring(0, Math.min(50, firstMsg.getMessage().getContent().length())),
                        firstMsg.getMessage().getMessageType());
            }
            
            // 自动标记消息为已读
            if (!messages.isEmpty()) {
                // 获取最新消息的ID
                Long latestMessageId = messages.getContent().stream()
                        .map(msg -> msg.getMessage().getId())
                        .max(Long::compareTo)
                        .orElse(0L);
                
                if (latestMessageId > 0) {
                    // 更新已读状态
                    readStatusService.updateReadStatus(userId, conversationId, latestMessageId);
                    log.info("Automatically marked messages as read up to ID {} for user {} in conversation {}", 
                            latestMessageId, userId, conversationId);
                }
            }
            
            // 创建带有额外信息的响应
            ApiResponse<Page<MessageResponse>> response = ApiResponse.success(messages);
            
            // 返回响应
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting conversation messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取会话消息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取消息历史记录
     * 
     * @param conversationId 会话ID
     * @param beforeMessageId 在此消息之前的消息
     * @param pageable 分页参数
     * @param authentication 认证信息
     * @return 消息列表
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessageHistory(
            @RequestParam Long conversationId,
            @RequestParam(required = false) Long beforeMessageId,
            Pageable pageable,
            Authentication authentication) {
        // TODO: 实现获取消息历史逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取消息状态
     * 
     * @param messageId 消息ID
     * @param authentication 认证信息
     * @return 消息状态
     */
    @GetMapping("/{messageId}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMessageStatus(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Getting status for message {} by user {}", messageId, userId);
            
            // 调用服务层获取消息状态
            Message message = messageService.getMessageById(messageId);
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查用户是否有权限查看该消息
            boolean hasAccess = conversationService.isUserInConversation(message.getConversationId(), userId);
            if (!hasAccess) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("无权访问此消息"));
            }
            
            // 返回消息状态
            Map<String, String> statusMap = new HashMap<>();
            statusMap.put("status", message.getStatus().name());
            
            return ResponseEntity.ok(ApiResponse.success(statusMap));
            
        } catch (Exception e) {
            log.error("Failed to get message status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("获取消息状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新消息状态
     * 
     * @param messageId 消息ID
     * @param request 更新状态请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/{messageId}/status")
    public ResponseEntity<ApiResponse<Void>> updateMessageStatus(
            @PathVariable Long messageId,
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            // 获取状态
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("状态不能为空"));
            }
            
            // 解析状态
            MessageStatus status;
            try {
                status = MessageStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("无效的状态值"));
            }
            
            log.info("Updating status for message {} to {} by user {}", messageId, status, userId);
            
            // 调用服务层更新消息状态
            Message message = messageService.getMessageById(messageId);
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查用户是否有权限更新该消息
            boolean hasAccess = message.getSenderId().equals(userId) || 
                               conversationService.isUserInConversation(message.getConversationId(), userId);
            if (!hasAccess) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("无权更新此消息状态"));
            }
            
                         // 更新状态
             message.setStatus(status);
             message.setUpdatedAt(LocalDateTime.now());
             messageRepository.save(message);
            
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (Exception e) {
            log.error("Failed to update message status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("更新消息状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取消息已读状态
     * 
     * @param messageId 消息ID
     * @param authentication 认证信息
     * @return 消息已读状态
     */
    @GetMapping("/{messageId}/read-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getMessageReadStatus(
            @PathVariable Long messageId,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Getting read status for message {} by user {}", messageId, userId);
            
            // 调用服务层获取消息
            Message message = messageService.getMessageById(messageId);
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查用户是否有权限查看该消息
            boolean hasAccess = conversationService.isUserInConversation(message.getConversationId(), userId);
            if (!hasAccess) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("无权访问此消息"));
            }
            
            // 返回消息已读状态
            Map<String, Boolean> readStatusMap = new HashMap<>();
            readStatusMap.put("isRead", message.getIsRead());
            
            return ResponseEntity.ok(ApiResponse.success(readStatusMap));
            
        } catch (Exception e) {
            log.error("Failed to get message read status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("获取消息已读状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取会话中的未读消息数量
     * 
     * @param conversationId 会话ID
     * @param authentication 认证信息
     * @return 未读消息数量
     */
    @GetMapping("/conversation/{conversationId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadMessageCount(
            @PathVariable Long conversationId,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Getting unread message count for user {} in conversation {}", userId, conversationId);
            
            // 使用新的ReadStatusService获取未读消息数量
            Long unreadCount = readStatusService.countUnreadMessages(userId, conversationId);
            
            return ResponseEntity.ok(ApiResponse.success(unreadCount));
        } catch (Exception e) {
            log.error("Error getting unread message count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.serverError("获取未读消息数量失败: " + e.getMessage()));
        }
    }
}