package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.group.dto.request.CreateGroupRequest;
import com.im.imcommunicationsystem.group.dto.request.UpdateGroupRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupResponse;
import com.im.imcommunicationsystem.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 群组控制器
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;

    /**
     * 创建群组
     */
    @PostMapping
    public ApiResponse<GroupResponse> createGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("创建群组，用户ID: {}, 群组名称: {}", userId, request.getName());
        GroupResponse group = groupService.createGroup(userId, request);
        return ApiResponse.success(group);
    }

    /**
     * 获取群组详情
     */
    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroupDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("获取群组详情，用户ID: {}, 群组ID: {}", userId, groupId);
        GroupResponse group = groupService.getGroupById(groupId, userId);
        return ApiResponse.success(group);
    }

    /**
     * 更新群组信息
     */
    @PutMapping("/{groupId}")
    public ApiResponse<GroupResponse> updateGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("更新群组信息，用户ID: {}, 群组ID: {}", userId, groupId);
        try {
            GroupResponse group = groupService.updateGroup(groupId, userId, request);
            return ApiResponse.success(group);
        } catch (Exception e) {
            log.error("更新群组信息失败: {}", e.getMessage(), e);
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取用户的群组列表
     */
    @GetMapping
    public ApiResponse<?> getUserGroups(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        try {
            Long userId = extractUserId(userDetails, headerUserId);
            
            if (userId == null) {
                log.warn("获取群组列表失败：未能获取用户ID");
                return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
            }
            
            log.info("获取用户群组列表，用户ID: {}, 页码: {}, 大小: {}", userId, page, size);
            
            try {
                Page<GroupResponse> groups = groupService.getUserGroups(userId, PageRequest.of(page, size));
                
                log.info("成功获取用户群组列表，用户ID: {}, 群组数量: {}", userId, groups.getContent().size());
                
                // 打印每个群组的信息，便于调试
                groups.getContent().forEach(group -> 
                    log.debug("群组信息: id={}, name={}, memberCount={}", 
                        group.getId(), group.getName(), group.getMemberCount())
                );
                
                // 创建一个包含更多信息的响应
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("content", groups.getContent());
                responseData.put("totalElements", groups.getTotalElements());
                responseData.put("totalPages", groups.getTotalPages());
                responseData.put("size", groups.getSize());
                responseData.put("number", groups.getNumber());
                responseData.put("first", groups.isFirst());
                responseData.put("last", groups.isLast());
                responseData.put("empty", groups.isEmpty());
                
                return ApiResponse.success(responseData);
            } catch (Exception e) {
                log.error("处理群组数据时出错: {}", e.getMessage(), e);
                
                // 创建一个空的响应，避免前端崩溃
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("content", new ArrayList<>());
                emptyResponse.put("totalElements", 0);
                emptyResponse.put("totalPages", 0);
                emptyResponse.put("size", size);
                emptyResponse.put("number", page);
                emptyResponse.put("first", true);
                emptyResponse.put("last", true);
                emptyResponse.put("empty", true);
                
                return ApiResponse.success(emptyResponse);
            }
        } catch (Exception e) {
            log.error("获取用户群组列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取群组列表失败: " + e.getMessage());
        }
    }

    /**
     * 解散群组
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolveGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("解散群组，用户ID: {}, 群组ID: {}", userId, groupId);
        
        try {
        boolean success = groupService.dissolveGroup(groupId, userId);
        
        if (success) {
                log.info("成功解散群组: groupId={}", groupId);
            return ApiResponse.success();
        } else {
                log.warn("解散群组失败，返回false: groupId={}", groupId);
            return ApiResponse.error(400, "解散群组失败");
            }
        } catch (Exception e) {
            log.error("解散群组过程中发生异常: groupId={}, 错误信息: {}", groupId, e.getMessage(), e);
            return ApiResponse.error(500, "解散群组失败: " + e.getMessage());
        }
    }

    /**
     * 检查当前用户是否是群组成员
     */
    @GetMapping("/{groupId}/members/check")
    public ApiResponse<Boolean> checkUserInGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        
        Long userId = extractUserId(userDetails, headerUserId);
        
        if (userId == null) {
            return ApiResponse.error(401, "未能获取用户ID，请确保已登录");
        }
        
        log.info("检查用户是否在群组中，用户ID: {}, 群组ID: {}", userId, groupId);
        
        try {
            boolean isMember = groupService.isGroupMember(groupId, userId);
            
            if (isMember) {
                log.info("用户 {} 是群组 {} 的成员", userId, groupId);
                return ApiResponse.success(true);
            } else {
                log.info("用户 {} 不是群组 {} 的成员", userId, groupId);
                return ApiResponse.error(403, "您不是该群组的成员");
            }
        } catch (Exception e) {
            log.error("检查用户是否在群组中失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "检查群组成员关系失败: " + e.getMessage());
        }
    }

    /**
     * 从安全上下文或请求头中提取用户ID
     * 
     * @param userDetails 安全上下文中的用户信息
     * @param headerUserId 请求头中的用户ID
     * @return 提取的用户ID，如果无法提取则返回null
     */
    private Long extractUserId(UserDetails userDetails, String headerUserId) {
        // 尝试从安全上下文中获取用户ID
        if (userDetails != null) {
            log.debug("从安全上下文中获取用户ID");
            return Long.valueOf(userDetails.getUsername());
        } 
        // 如果安全上下文中没有用户信息，尝试从请求头中获取
        else if (headerUserId != null && !headerUserId.isEmpty()) {
            log.debug("从请求头X-User-Id中获取用户ID: {}", headerUserId);
            try {
                return Long.valueOf(headerUserId);
            } catch (NumberFormatException e) {
                log.error("请求头中的用户ID格式不正确: {}", headerUserId, e);
                return null;
            }
        } else {
            log.error("未能获取用户ID，安全上下文和请求头中均无有效信息");
            return null;
        }
    }
} 