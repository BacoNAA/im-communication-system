package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.group.dto.response.GroupJoinRequestResponse;
import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import com.im.imcommunicationsystem.group.service.GroupJoinRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 群组加入请求控制器
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupJoinRequestController {

    private final GroupJoinRequestService joinRequestService;

    /**
     * 获取群组的加入请求列表
     */
    @GetMapping("/{groupId}/join-requests")
    public ApiResponse<?> getGroupJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("获取群组加入请求列表: groupId={}, userId={}, status={}, page={}, size={}",
                groupId, userId, status, page, size);
        
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            GroupJoinRequestStatus requestStatus = null;
            
            if (status != null) {
                try {
                    requestStatus = GroupJoinRequestStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.error(400, "无效的请求状态");
                }
            }
            
            Page<GroupJoinRequestResponse> result = joinRequestService.getGroupJoinRequests(
                    groupId, requestStatus, userId, pageRequest);
            
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
            log.error("获取群组加入请求列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取群组加入请求列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的加入请求列表
     */
    @GetMapping("/my-join-requests")
    public ApiResponse<?> getUserJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("获取用户加入请求列表: userId={}, status={}, page={}, size={}",
                userId, status, page, size);
        
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            GroupJoinRequestStatus requestStatus = null;
            
            if (status != null) {
                try {
                    requestStatus = GroupJoinRequestStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.error(400, "无效的请求状态");
                }
            }
            
            Page<GroupJoinRequestResponse> result = joinRequestService.getUserJoinRequests(
                    userId, requestStatus, pageRequest);
            
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
            log.error("获取用户加入请求列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取用户加入请求列表失败: " + e.getMessage());
        }
    }

    /**
     * 处理加入请求（批准或拒绝）
     */
    @PutMapping("/{groupId}/join-requests/{requestId}")
    public ApiResponse<?> handleJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long requestId,
            @RequestParam boolean approve,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("处理群组加入请求: groupId={}, requestId={}, userId={}, approve={}",
                groupId, requestId, userId, approve);
        
        try {
            boolean success = joinRequestService.handleJoinRequest(requestId, groupId, userId, approve);
            
            if (success) {
                return ApiResponse.success(approve ? "已批准加入请求" : "已拒绝加入请求");
            } else {
                return ApiResponse.error(400, "处理加入请求失败");
            }
        } catch (Exception e) {
            log.error("处理加入请求失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "处理加入请求失败: " + e.getMessage());
        }
    }

    /**
     * 取消加入请求
     */
    @DeleteMapping("/join-requests/{requestId}")
    public ApiResponse<?> cancelJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("取消群组加入请求: requestId={}, userId={}", requestId, userId);
        
        try {
            boolean success = joinRequestService.cancelJoinRequest(requestId, userId);
            
            if (success) {
                return ApiResponse.success("已取消加入请求");
            } else {
                return ApiResponse.error(400, "取消加入请求失败");
            }
        } catch (Exception e) {
            log.error("取消加入请求失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "取消加入请求失败: " + e.getMessage());
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