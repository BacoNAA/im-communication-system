package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.group.dto.response.GroupMemberResponse;
import com.im.imcommunicationsystem.group.service.GroupMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群成员控制器
 */
@RestController
@RequestMapping("/api/groups/{groupId}/members")
@RequiredArgsConstructor
@Slf4j
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    /**
     * 获取群成员列表
     */
    @GetMapping
    public ApiResponse<Page<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<GroupMemberResponse> members = groupMemberService.getGroupMembers(groupId, PageRequest.of(page, size));
        return ApiResponse.success(members);
    }

    /**
     * 获取群管理员列表
     */
    @GetMapping("/admins")
    public ApiResponse<List<GroupMemberResponse>> getGroupAdmins(@PathVariable Long groupId) {
        List<GroupMemberResponse> admins = groupMemberService.getGroupAdmins(groupId);
        return ApiResponse.success(admins);
    }

    /**
     * 添加群成员
     */
    @PostMapping
    public ApiResponse<List<GroupMemberResponse>> addMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @RequestBody Map<String, List<Long>> request,
            HttpServletRequest httpRequest) {
        
        log.info("收到添加群成员请求: groupId={}, request={}", groupId, request);
        
        try {
            // 从请求头中获取用户ID
            Long operatorId = null;
            
            // 首先尝试从UserDetails获取
            if (userDetails != null) {
                operatorId = Long.valueOf(userDetails.getUsername());
                log.info("从UserDetails获取操作者ID: {}", operatorId);
            } 
            
            // 如果UserDetails为空，尝试从请求头获取
            if (operatorId == null) {
                String userIdHeader = httpRequest.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    try {
                        operatorId = Long.valueOf(userIdHeader);
                        log.info("从请求头获取操作者ID: {}", operatorId);
                    } catch (NumberFormatException e) {
                        log.warn("请求头中的用户ID格式不正确: {}", userIdHeader);
                    }
                }
            }
            
            // 如果仍然为空，返回未授权错误
            if (operatorId == null) {
                log.warn("无法获取操作者ID，请求未授权");
                return ApiResponse.unauthorized("未授权，请先登录");
            }
            
            List<Long> memberIds = request.get("memberIds");
            
            if (memberIds == null || memberIds.isEmpty()) {
                return ApiResponse.error(400, "成员ID列表不能为空");
            }
            
            List<GroupMemberResponse> members = groupMemberService.addMembers(groupId, memberIds, operatorId);
            return ApiResponse.success(members);
        } catch (Exception e) {
            log.error("添加群成员异常: groupId={}, error={}", groupId, e.getMessage(), e);
            return ApiResponse.serverError("添加群成员失败: " + e.getMessage());
        }
    }

    /**
     * 移除群成员
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        log.info("收到移除群成员请求: groupId={}, userId={}", groupId, userId);
        
        try {
            // 从请求头中获取用户ID
            Long operatorId = null;
            
            // 首先尝试从UserDetails获取
            if (userDetails != null) {
                operatorId = Long.valueOf(userDetails.getUsername());
                log.info("从UserDetails获取操作者ID: {}", operatorId);
            } 
            
            // 如果UserDetails为空，尝试从请求头获取
            if (operatorId == null) {
                String userIdHeader = httpRequest.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    try {
                        operatorId = Long.valueOf(userIdHeader);
                        log.info("从请求头获取操作者ID: {}", operatorId);
                    } catch (NumberFormatException e) {
                        log.warn("请求头中的用户ID格式不正确: {}", userIdHeader);
                    }
                }
            }
            
            // 如果仍然为空，返回未授权错误
            if (operatorId == null) {
                log.warn("无法获取操作者ID，请求未授权");
                return ApiResponse.unauthorized("未授权，请先登录");
            }
            
            boolean success = groupMemberService.removeMember(groupId, userId, operatorId);
            
            if (success) {
                log.info("移除群成员成功: groupId={}, userId={}, operatorId={}", groupId, userId, operatorId);
                return ApiResponse.success();
            } else {
                log.warn("移除群成员失败: groupId={}, userId={}, operatorId={}", groupId, userId, operatorId);
                return ApiResponse.error(400, "移除成员失败");
            }
        } catch (Exception e) {
            log.error("移除群成员异常: groupId={}, userId={}, error={}", groupId, userId, e.getMessage(), e);
            return ApiResponse.serverError("移除群成员失败: " + e.getMessage());
        }
    }

    /**
     * 设置或取消管理员
     */
    @PutMapping("/{userId}/admin")
    public ApiResponse<Void> setAdmin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request,
            HttpServletRequest httpRequest) {
        
        log.info("收到设置管理员请求: groupId={}, userId={}, request={}", groupId, userId, request);
        
        try {
            // 从请求头中获取用户ID
            Long operatorId = null;
            
            // 首先尝试从UserDetails获取
            if (userDetails != null) {
                operatorId = Long.valueOf(userDetails.getUsername());
                log.info("从UserDetails获取操作者ID: {}", operatorId);
            } 
            
            // 如果UserDetails为空，尝试从请求头获取
            if (operatorId == null) {
                String userIdHeader = httpRequest.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    try {
                        operatorId = Long.valueOf(userIdHeader);
                        log.info("从请求头获取操作者ID: {}", operatorId);
                    } catch (NumberFormatException e) {
                        log.warn("请求头中的用户ID格式不正确: {}", userIdHeader);
                    }
                }
            }
            
            // 如果仍然为空，返回未授权错误
            if (operatorId == null) {
                log.warn("无法获取操作者ID，请求未授权");
                return ApiResponse.unauthorized("未授权，请先登录");
            }
            
            Boolean isAdmin = request.get("isAdmin");
            log.info("isAdmin参数: {}", isAdmin);
            
            if (isAdmin == null) {
                log.warn("缺少isAdmin参数: request={}", request);
                return ApiResponse.error(400, "缺少isAdmin参数");
            }
            
            boolean success = groupMemberService.setAdmin(groupId, userId, isAdmin, operatorId);
            
            if (success) {
                log.info("设置管理员成功: groupId={}, userId={}, isAdmin={}, operatorId={}", 
                        groupId, userId, isAdmin, operatorId);
                return ApiResponse.success();
            } else {
                log.warn("设置管理员失败: groupId={}, userId={}, isAdmin={}, operatorId={}", 
                        groupId, userId, isAdmin, operatorId);
                return ApiResponse.error(400, "设置管理员失败");
            }
        } catch (Exception e) {
            log.error("设置管理员异常: groupId={}, userId={}, error={}", 
                    groupId, userId, e.getMessage(), e);
            return ApiResponse.serverError("设置管理员失败: " + e.getMessage());
        }
    }

    /**
     * 设置成员禁言状态
     */
    @PutMapping("/{userId}/mute")
    public ApiResponse<Void> setMemberMuteStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        log.info("收到设置禁言状态请求: groupId={}, userId={}, request={}", groupId, userId, request);
        
        try {
            // 从请求头中获取用户ID
            Long operatorId = null;
            
            // 首先尝试从UserDetails获取
            if (userDetails != null) {
                operatorId = Long.valueOf(userDetails.getUsername());
                log.info("从UserDetails获取操作者ID: {}", operatorId);
            } 
            
            // 如果UserDetails为空，尝试从请求头获取
            if (operatorId == null) {
                String userIdHeader = httpRequest.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    try {
                        operatorId = Long.valueOf(userIdHeader);
                        log.info("从请求头获取操作者ID: {}", operatorId);
                    } catch (NumberFormatException e) {
                        log.warn("请求头中的用户ID格式不正确: {}", userIdHeader);
                    }
                }
            }
            
            // 如果仍然为空，返回未授权错误
            if (operatorId == null) {
                log.warn("无法获取操作者ID，请求未授权");
                return ApiResponse.unauthorized("未授权，请先登录");
            }
            
            Boolean isMuted = (Boolean) request.get("isMuted");
            Integer minutes = (Integer) request.get("minutes");
            
            if (isMuted == null) {
                return ApiResponse.error(400, "缺少isMuted参数");
            }
            
            boolean success = groupMemberService.setMemberMuteStatus(groupId, userId, isMuted, minutes, operatorId);
            
            if (success) {
                log.info("设置禁言状态成功: groupId={}, userId={}, isMuted={}, minutes={}, operatorId={}", 
                        groupId, userId, isMuted, minutes, operatorId);
                return ApiResponse.success();
            } else {
                log.warn("设置禁言状态失败: groupId={}, userId={}, isMuted={}, operatorId={}", 
                        groupId, userId, isMuted, operatorId);
                return ApiResponse.error(400, "设置禁言状态失败");
            }
        } catch (Exception e) {
            log.error("设置禁言状态异常: groupId={}, userId={}, error={}", groupId, userId, e.getMessage(), e);
            return ApiResponse.serverError("设置禁言状态失败: " + e.getMessage());
        }
    }
} 