package com.im.imcommunicationsystem.message.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.message.dto.request.GlobalSearchRequest;
import com.im.imcommunicationsystem.message.dto.request.ConversationSearchRequest;
import com.im.imcommunicationsystem.message.dto.response.MessageSearchResponse;
import com.im.imcommunicationsystem.message.service.MessageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 消息搜索控制器
 * 处理消息搜索相关的HTTP请求
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/messages/search")
@RequiredArgsConstructor
@Slf4j
public class MessageSearchController {

    private final MessageSearchService messageSearchService;

    /**
     * 会话内搜索
     * 
     * @param request 会话内搜索请求
     * @param pageable 分页参数
     * @param httpRequest HTTP请求
     * @return 搜索结果
     */
    @PostMapping("/conversation")
    public ResponseEntity<ApiResponse<Page<MessageSearchResponse>>> searchInConversation(
            @Valid @RequestBody ConversationSearchRequest request,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received conversation search request: conversationId={}, keyword={}",
                    request.getConversationId(), request.getKeyword());
            
            // 获取当前用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.warn("User not authenticated for conversation search");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 创建排序和分页对象
            Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy()
            );
            
            Pageable pageRequest = PageRequest.of(
                request.getPage(), 
                request.getSize(),
                sort
            );
            
            // 调用服务执行搜索
            Page<MessageSearchResponse> results = messageSearchService.searchInConversation(
                request, pageRequest, userId
            );
            
            log.info("Search completed: found {} results", 
                    results != null ? results.getTotalElements() : 0);
            
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error during conversation search: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 全局搜索
     * 
     * @param request 全局搜索请求
     * @param pageable 分页参数
     * @param httpRequest HTTP请求
     * @return 搜索结果
     */
    @PostMapping("/global")
    public ResponseEntity<ApiResponse<MessageSearchResponse>> globalSearch(
            @Valid @RequestBody GlobalSearchRequest request,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received global search request: keyword={}", request.getKeyword());
            
            // 获取当前用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.warn("User not authenticated for global search");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 创建排序和分页对象
            Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy()
            );
            
            Pageable pageRequest = PageRequest.of(
                request.getPage(), 
                request.getSize(),
                sort
            );
            
            // 调用服务执行搜索
            Page<MessageSearchResponse> resultsPage = messageSearchService.globalSearch(
                request, pageRequest, userId
            );
            
            // 从Page中提取MessageSearchResponse对象
            MessageSearchResponse searchResponse;
            if (resultsPage != null && resultsPage.hasContent()) {
                searchResponse = resultsPage.getContent().get(0);
                log.info("Global search completed: found {} results", searchResponse.getTotal());
            } else {
                // 创建一个空的搜索响应
                searchResponse = MessageSearchResponse.builder()
                    .results(java.util.Collections.emptyList())
                    .total(0L)
                    .page(request.getPage())
                    .size(request.getSize())
                    .totalPages(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .keyword(request.getKeyword())
                    .searchTime(System.currentTimeMillis())
                    .build();
                log.info("Global search completed: no results found");
            }
            
            return ResponseEntity.ok(ApiResponse.success(searchResponse));
        } catch (Exception e) {
            log.error("Error during global search: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("全局搜索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 会话内搜索 (GET方法)
     * 
     * @param conversationId 会话ID
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param highlight 是否高亮
     * @param pageable 分页参数
     * @param httpRequest HTTP请求
     * @return 搜索结果
     */
    @GetMapping("/conversation")
    public ResponseEntity<ApiResponse<MessageSearchResponse>> searchInConversationGet(
            @RequestParam Long conversationId,
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "true") Boolean highlight,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Received GET conversation search request: conversationId={}, keyword={}",
                    conversationId, keyword);
            
            // 获取当前用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.warn("User not authenticated for conversation search");
                return ResponseEntity.status(401)
                        .body(ApiResponse.unauthorized("用户未登录"));
            }
            
            // 创建搜索请求对象
            ConversationSearchRequest request = ConversationSearchRequest.builder()
                .conversationId(conversationId)
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .highlight(highlight)
                .build();
            
            // 创建排序和分页对象
            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("asc") ? 
                    Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
            );
            
            Pageable pageRequest = PageRequest.of(
                page, 
                size,
                sort
            );
            
            // 调用服务执行搜索
            Page<MessageSearchResponse> resultsPage = messageSearchService.searchInConversation(
                request, pageRequest, userId
            );
            
            // 从Page中提取MessageSearchResponse对象
            MessageSearchResponse searchResponse = null;
            if (resultsPage != null && resultsPage.hasContent()) {
                searchResponse = resultsPage.getContent().get(0);
                log.info("GET Search completed: found {} results", searchResponse.getTotal());
            } else {
                // 创建一个空的搜索响应
                searchResponse = MessageSearchResponse.builder()
                    .results(java.util.Collections.emptyList())
                    .total(0L)
                    .page(page)
                    .size(size)
                    .totalPages(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .keyword(keyword)
                    .searchTime(System.currentTimeMillis())
                    .build();
                log.info("GET Search completed: no results found");
            }
            
            return ResponseEntity.ok(ApiResponse.success(searchResponse));
        } catch (Exception e) {
            log.error("Error during GET conversation search: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("搜索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从请求中获取当前用户ID
     * 
     * @param request HTTP请求
     * @return 用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 从请求头获取用户ID
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader);
            }
        }
        
        // 从认证上下文获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                // 这里需要通过用户名查询用户ID，但由于没有直接注入UserRepository
                // 所以返回null，实际项目中应该实现这个逻辑
                log.warn("Unable to get user ID from UserDetails, username: {}", username);
            }
        }
        
        return null;
    }
}