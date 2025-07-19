package com.im.imcommunicationsystem.message.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.ConversationResponse;
import com.im.imcommunicationsystem.message.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.dto.response.ConversationDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话控制器
 * 处理会话管理相关的HTTP请求
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 获取会话列表
     * 
     * @param pageable 分页参数
     * @param authentication 认证信息
     * @return 会话列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
            Pageable pageable,
            HttpServletRequest request) {
        Long userId = null;
        try {
            log.info("开始处理获取会话列表请求: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize());
            
            // 从请求中获取用户ID（假设通过JWT或其他方式获取）
            userId = getCurrentUserId(request);
            log.info("从请求中获取到用户ID: userId={}", userId);
            
            if (userId == null) {
                log.warn("用户未登录，无法获取会话列表");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 调用服务层获取会话列表
            log.info("调用服务层获取会话列表: userId={}, page={}, size={}", 
                    userId, pageable.getPageNumber(), pageable.getPageSize());
            
            // 先检查用户是否存在会话
            List<Conversation> userConversations = conversationService.getConversationsByUserId(userId);
            log.info("用户参与的会话数量: {}", userConversations.size());
            
            // 输出会话ID列表，帮助调试
            if (!userConversations.isEmpty()) {
                String conversationIds = userConversations.stream()
                        .map(c -> c.getId().toString())
                        .collect(Collectors.joining(", "));
                log.info("用户参与的会话ID列表: [{}]", conversationIds);
            } else {
                log.info("用户未参与任何会话");
            }
            
            // 获取分页的会话列表
            Page<ConversationResponse> conversations = conversationService.getConversations(pageable, userId);
            
            log.info("成功获取会话列表: userId={}, totalElements={}", 
                    userId, conversations.getTotalElements());
            
            // 检查返回的数据结构
            if (conversations.hasContent()) {
                log.debug("会话列表第一页内容: {}", conversations.getContent());
            }
            
            return ResponseEntity.ok(ApiResponse.success(conversations));
        } catch (Exception e) {
            log.error("获取会话列表失败: userId={}, error={}", userId, e.getMessage(), e);
            
            // 根据异常类型返回不同的错误响应
            if (e instanceof IllegalArgumentException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("请求参数错误: " + e.getMessage()));
            } else if (e instanceof SecurityException) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("无权限访问会话列表"));
            } else {
                return ResponseEntity.status(500)
                        .body(ApiResponse.serverError("获取会话列表失败: " + e.getMessage()));
            }
        }
    }

    /**
     * 获取已归档的会话列表
     * 
     * @param pageable 分页参数
     * @param request HTTP请求
     * @return 已归档的会话列表
     */
    @GetMapping("/archived")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getArchivedConversations(
            Pageable pageable,
            HttpServletRequest request) {
        Long userId = null;
        try {
            log.info("开始处理获取已归档会话列表请求: page={}, size={}", 
                    pageable.getPageNumber(), pageable.getPageSize());
            
            // 从请求中获取用户ID
            userId = getCurrentUserId(request);
            log.info("从请求中获取到用户ID: userId={}", userId);
            
            if (userId == null) {
                log.warn("用户未登录，无法获取已归档会话列表");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 调用服务层获取已归档会话列表
            Page<ConversationResponse> conversations = conversationService.getArchivedConversations(pageable, userId);
            
            log.info("成功获取已归档会话列表: userId={}, totalElements={}", 
                    userId, conversations.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(conversations));
        } catch (Exception e) {
            log.error("获取已归档会话列表失败: userId={}, error={}", userId, e.getMessage(), e);
            
            if (e instanceof IllegalArgumentException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("请求参数错误: " + e.getMessage()));
            } else if (e instanceof SecurityException) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("无权限访问已归档会话列表"));
            } else {
                return ResponseEntity.status(500)
                        .body(ApiResponse.serverError("获取已归档会话列表失败: " + e.getMessage()));
            }
        }
    }

    /**
     * 创建会话
     * 
     * @param request 创建会话请求
     * @param authentication 认证信息
     * @return 会话响应
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            Authentication authentication) {
        // TODO: 实现创建会话逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 更新会话设置
     * 
     * @param conversationId 会话ID
     * @param request 更新会话设置请求
     * @param authentication 认证信息
     * @return 会话响应
     */
    @PutMapping("/{conversationId}/settings")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateConversationSettings(
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateConversationSettingsRequest request,
            Authentication authentication) {
        // TODO: 实现更新会话设置逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 置顶/取消置顶会话
     * 
     * @param conversationId 会话ID
     * @param request 置顶请求
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PutMapping("/{conversationId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody PinConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录"));
            }
            
            conversationService.pinConversation(conversationId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("置顶会话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("置顶会话失败: " + e.getMessage()));
        }
    }

    /**
     * 归档/取消归档会话
     * 
     * @param conversationId 会话ID
     * @param request 归档请求
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PutMapping("/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody ArchiveConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录"));
            }
            
            conversationService.archiveConversation(conversationId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("归档会话失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("归档会话失败: " + e.getMessage()));
        }
    }

    /**
     * 设置会话免打扰状态
     * 
     * @param conversationId 会话ID
     * @param request 免打扰请求
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PutMapping("/{conversationId}/mute")
    public ResponseEntity<ApiResponse<Void>> muteConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody MuteConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录"));
            }
            
            conversationService.muteConversation(conversationId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("设置会话免打扰状态失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("设置会话免打扰状态失败: " + e.getMessage()));
        }
    }

    /**
     * 保存草稿
     * 
     * @param conversationId 会话ID
     * @param request 保存草稿请求
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PutMapping("/{conversationId}/draft")
    public ResponseEntity<ApiResponse<Void>> saveDraft(
            @PathVariable Long conversationId,
            @Valid @RequestBody SaveDraftRequest request,
            Authentication authentication) {
        // TODO: 实现保存草稿逻辑
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 获取单个会话信息
     * 
     * @param conversationId 会话ID
     * @param request HTTP请求
     * @return 会话信息
     */
    @GetMapping("/private/{contactId}")
    public ResponseEntity<ApiResponse<ConversationDTO>> getOrCreatePrivateConversation(
            @PathVariable Long contactId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.unauthorized("用户未登录"));
            }
            if (userId.equals(contactId)) {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("不能与自己创建私聊"));
            }
            Conversation conversation = conversationService.getOrCreatePrivateConversation(userId, contactId);
            ConversationDTO dto = conversationService.buildConversationDTO(conversation, userId);
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception e) {
            log.error("获取或创建私聊会话失败: contactId={}, error={}", contactId, e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.serverError("获取或创建私聊会话失败"));
        }
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        Long userId = null;
        try {
            log.info("开始获取会话信息: conversationId={}", conversationId);
            
            // 从请求中获取用户ID
            userId = getCurrentUserId(request);
            log.info("从请求中获取到用户ID: userId={}", userId);
            
            if (userId == null) {
                log.warn("用户未登录，无法获取会话信息");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 检查用户是否有权限访问该会话
            if (!conversationService.isUserInConversation(conversationId, userId)) {
                log.warn("用户无权限访问会话: userId={}, conversationId={}", userId, conversationId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("无权限访问该会话"));
            }
            
            // 获取会话信息
            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                log.warn("会话不存在: conversationId={}", conversationId);
                return ResponseEntity.status(404)
                        .body(ApiResponse.notFound("会话不存在"));
            }
            
            log.info("成功获取会话: conversationId={}, type={}", conversation.getId(), conversation.getConversationType());
            
            // 构建ConversationDTO
            var conversationDTO = conversationService.buildConversationDTO(conversation, userId);
            if (conversationDTO == null) {
                log.error("构建ConversationDTO失败: conversationId={}", conversationId);
                return ResponseEntity.status(500)
                        .body(ApiResponse.serverError("构建会话信息失败"));
            }
            
            log.info("成功构建ConversationDTO: conversationId={}, displayName={}", conversationDTO.getId(), conversationDTO.getDisplayName());
            
            // 返回成功响应
            var response = ConversationResponse.success(conversationDTO);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取会话信息失败: userId={}, conversationId={}, error={}", userId, conversationId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("获取会话信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取或创建私聊会话
     * 
     * @param friendId 好友ID
     * @param request HTTP请求
     * @return 会话信息
     */
    @PostMapping("/private/{contactId}")
    public ResponseEntity<ConversationResponse> createPrivateConversation(
            @PathVariable Long contactId,
            HttpServletRequest request) {
        Long userId = null;
        try {
            log.info("开始处理获取或创建私聊会话请求: contactId={}", contactId);
            
            // 从请求中获取用户ID
            userId = getCurrentUserId(request);
            log.info("从请求中获取到用户ID: userId={}", userId);
            
            if (userId == null) {
                log.warn("用户未登录，无法创建会话");
                return ResponseEntity.status(401)
                        .body(ConversationResponse.error("用户未登录"));
            }
            
            log.info("开始获取或创建私聊会话: userId={}, contactId={}", userId, contactId);
            
            // 获取或创建私聊会话
            var conversation = conversationService.getOrCreatePrivateConversation(userId, contactId);
            if (conversation == null) {
                log.error("conversationService.getOrCreatePrivateConversation返回null: userId={}, contactId={}", userId, contactId);
                return ResponseEntity.status(500)
                        .body(ConversationResponse.error("创建会话失败"));
            }
            
            log.info("成功获取或创建私聊会话: conversationId={}, type={}", conversation.getId(), conversation.getConversationType());
            
            // 构建ConversationDTO
            log.info("开始构建ConversationDTO");
            var conversationDTO = conversationService.buildConversationDTO(conversation, userId);
            if (conversationDTO == null) {
                log.error("构建ConversationDTO失败: conversationId={}", conversation.getId());
                return ResponseEntity.status(500)
                        .body(ConversationResponse.error("构建会话信息失败"));
            }
            
            log.info("成功构建ConversationDTO: conversationId={}, displayName={}", conversationDTO.getId(), conversationDTO.getDisplayName());
            
            // 返回成功响应
            var response = ConversationResponse.success(conversationDTO);
            log.info("成功创建响应，准备返回: success={}", response.getSuccess());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取或创建私聊会话失败: userId={}, contactId={}, error={}", userId, contactId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ConversationResponse.error("获取或创建私聊会话失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从请求中获取当前用户ID
     * 
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        log.info("开始从请求中获取用户ID");
        
        // 1. 尝试从请求参数中获取
        String userIdStr = request.getParameter("userId");
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                log.info("从请求参数中获取到用户ID: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.warn("请求参数中的userId格式不正确: {}", userIdStr);
            }
        }
        
        // 2. 尝试从请求头中获取
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                log.info("从请求头X-User-Id中获取到用户ID: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.warn("请求头X-User-Id格式不正确: {}", userIdHeader);
            }
        }
        
        // 3. 尝试从认证信息中获取
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    String username = ((UserDetails) principal).getUsername();
                    // 假设用户名是邮箱，通过邮箱查询用户ID
                    // 这里需要注入UserRepository或者使用其他方式获取用户ID
                    log.info("从认证信息中获取到用户名: {}", username);
                    // 这里应该是通过用户名查询用户ID的逻辑
                    // 由于没有直接注入UserRepository，这里只是示例
                    // return userRepository.findByEmail(username).map(User::getId).orElse(null);
                }
                
                // 如果Principal中直接包含用户ID
                if (authentication.getDetails() instanceof Map) {
                    Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                    if (details.containsKey("userId")) {
                        Object userId = details.get("userId");
                        if (userId instanceof Long) {
                            log.info("从认证信息details中获取到用户ID: {}", userId);
                            return (Long) userId;
                        } else if (userId instanceof String) {
                            try {
                                Long id = Long.parseLong((String) userId);
                                log.info("从认证信息details中获取到用户ID(字符串转换): {}", id);
                                return id;
                            } catch (NumberFormatException e) {
                                log.warn("认证信息中的userId格式不正确: {}", userId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("从认证信息中获取用户ID失败: {}", e.getMessage(), e);
        }
        
        log.warn("无法从请求中获取有效的用户ID");
        return null;
    }
}