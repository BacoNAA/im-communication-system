package com.im.imcommunicationsystem.message.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.MessageResponse;
import com.im.imcommunicationsystem.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        // TODO: 实现撤回消息逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
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
        // TODO: 实现编辑消息逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
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
        // TODO: 实现转发消息逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 标记消息为已读
     * 
     * @param request 标记已读请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Valid @RequestBody MarkAsReadRequest request,
            Authentication authentication) {
        // TODO: 实现标记已读逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取会话消息列表
     * 
     * @param conversationId 会话ID
     * @param requestUserId 用户ID（查询参数，可选）
     * @param pageable 分页参数
     * @param authentication 认证信息
     * @return 消息列表
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            Pageable pageable,
            Authentication authentication) {
        try {
            // 获取当前用户ID
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                log.warn("Failed to get user ID from authentication");
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("用户认证失败"));
            }
            
            log.info("Getting messages for conversation {} by user {}", conversationId, userId);
            
            // 调用服务层获取消息
            Page<MessageResponse> messages = messageService.getConversationMessages(conversationId, userId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(messages));
            
        } catch (Exception e) {
            log.error("Failed to get conversation messages: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("获取消息失败: " + e.getMessage()));
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
}