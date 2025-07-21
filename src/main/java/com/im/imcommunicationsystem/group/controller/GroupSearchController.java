package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.group.dto.request.GroupSearchRequest;
import com.im.imcommunicationsystem.group.dto.request.JoinGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupSearchResponse;
import com.im.imcommunicationsystem.group.service.GroupJoinRequestService;
import com.im.imcommunicationsystem.group.service.GroupSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 群组搜索控制器
 */
@RestController
@RequestMapping("/api/groups/search")
@RequiredArgsConstructor
@Slf4j
public class GroupSearchController {

    private final GroupSearchService groupSearchService;
    private final GroupJoinRequestService groupJoinRequestService;

    /**
     * 搜索群组
     */
    @PostMapping
    public ApiResponse<?> searchGroups(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GroupSearchRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("搜索群组: keyword={}, userId={}, page={}, size={}", 
                request.getKeyword(), userId, request.getPage(), request.getSize());
        
        try {
            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
            Page<GroupSearchResponse> result = groupSearchService.searchGroups(request, userId, pageRequest);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("content", result.getContent());
            responseData.put("totalElements", result.getTotalElements());
            responseData.put("totalPages", result.getTotalPages());
            responseData.put("size", result.getSize());
            responseData.put("number", result.getNumber());
            responseData.put("first", result.isFirst());
            responseData.put("last", result.isLast());
            responseData.put("empty", result.isEmpty());
            
            return ApiResponse.success(responseData);
        } catch (Exception e) {
            log.error("搜索群组失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "搜索群组失败: " + e.getMessage());
        }
    }

    /**
     * 获取群组详情（不需要成员资格）
     */
    @GetMapping("/{groupId}")
    public ApiResponse<GroupSearchResponse> getGroupDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("获取群组搜索详情: groupId={}, userId={}", groupId, userId);
        
        try {
            GroupSearchResponse response = groupSearchService.getGroupById(groupId, userId);
            
            if (response == null) {
                return ApiResponse.error(404, "群组不存在");
            }
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取群组详情失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取群组详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 申请加入群组
     */
    @PostMapping("/join")
    public ApiResponse<?> applyToJoinGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinGroupRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("申请加入群组: groupId={}, userId={}", request.getGroupId(), userId);
        
        try {
            boolean success = groupJoinRequestService.applyToJoinGroup(request, userId);
            
            if (success) {
                return ApiResponse.success("申请已发送，请等待管理员审批");
            } else {
                return ApiResponse.error(400, "申请加入群组失败");
            }
        } catch (Exception e) {
            log.error("申请加入群组失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "申请加入群组失败: " + e.getMessage());
        }
    }
    
    /**
     * 从身份认证信息或请求头中提取用户ID
     */
    private Long extractUserId(UserDetails userDetails, String headerUserId) {
        if (headerUserId != null) {
            try {
                return Long.parseLong(headerUserId);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", headerUserId);
            }
        }
        
        if (userDetails != null) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.warn("Invalid username as user ID: {}", userDetails.getUsername());
            }
        }
        
        return null;
    }
} 