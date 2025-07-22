package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationDetailResponse;
import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationResponse;
import com.im.imcommunicationsystem.admin.service.SystemNotificationService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户系统通知控制器
 * 处理用户拉取系统通知和标记已读的API
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class UserNotificationController {

    private final SystemNotificationService systemNotificationService;
    private final SecurityUtils securityUtils;

    /**
     * 获取用户系统通知列表
     *
     * @param pageable 分页信息
     * @param unreadOnly 是否只获取未读通知
     * @return 系统通知列表
     */
    @GetMapping
    public ResponseEntity<ResponseUtils.ApiResponse<Page<SystemNotificationResponse>>> getUserNotifications(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        log.info("获取用户通知列表，用户ID: {}，只看未读: {}", userId, unreadOnly);
        Page<SystemNotificationResponse> notifications = systemNotificationService.getUserNotifications(userId, pageable, unreadOnly);
        
        return ResponseEntity.ok(ResponseUtils.success("获取用户通知列表成功", notifications));
    }

    /**
     * 获取通知详情
     *
     * @param notificationId 通知ID
     * @return 通知详情
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<ResponseUtils.ApiResponse<SystemNotificationDetailResponse>> getNotificationDetail(
            @PathVariable Long notificationId) {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        log.info("获取通知详情，用户ID: {}，通知ID: {}", userId, notificationId);
        SystemNotificationDetailResponse notification = systemNotificationService.getUserNotificationDetail(userId, notificationId);
        
        // 移除自动标记已读，需要用户点击确认后才标记为已读
        
        return ResponseEntity.ok(ResponseUtils.success("获取通知详情成功", notification));
    }

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @return 成功响应
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ResponseUtils.ApiResponse<String>> markAsRead(
            @PathVariable Long notificationId) {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        log.info("标记通知为已读，用户ID: {}，通知ID: {}", userId, notificationId);
        boolean success = systemNotificationService.markNotificationAsRead(userId, notificationId);
        
        if (success) {
            return ResponseEntity.ok(ResponseUtils.success("标记已读成功"));
        } else {
            return ResponseEntity.badRequest().body(ResponseUtils.error("标记已读失败，通知可能不存在"));
        }
    }

    /**
     * 批量标记通知为已读
     *
     * @param request 请求体，包含通知ID列表
     * @return 成功响应
     */
    @PostMapping("/read-multiple")
    public ResponseEntity<ResponseUtils.ApiResponse<Map<String, Integer>>> markMultipleAsRead(
            @RequestBody Map<String, List<Long>> request) {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        List<Long> notificationIds = request.get("notificationIds");
        if (notificationIds == null || notificationIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("通知ID列表不能为空"));
        }
        
        log.info("批量标记通知为已读，用户ID: {}，通知数: {}", userId, notificationIds.size());
        int updatedCount = systemNotificationService.markMultipleNotificationsAsRead(userId, notificationIds);
        
        return ResponseEntity.ok(ResponseUtils.success("批量标记已读成功", Map.of("updatedCount", updatedCount)));
    }

    /**
     * 标记所有通知为已读
     *
     * @return 成功响应
     */
    @PostMapping("/read-all")
    public ResponseEntity<ResponseUtils.ApiResponse<Map<String, Integer>>> markAllAsRead() {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        log.info("标记所有通知为已读，用户ID: {}", userId);
        int updatedCount = systemNotificationService.markAllNotificationsAsRead(userId);
        
        return ResponseEntity.ok(ResponseUtils.success("标记所有通知为已读成功", Map.of("updatedCount", updatedCount)));
    }

    /**
     * 获取未读通知数量
     *
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ResponseUtils.ApiResponse<Map<String, Long>>> getUnreadCount() {
        
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("用户未登录"));
        }
        
        log.info("获取未读通知数量，用户ID: {}", userId);
        long unreadCount = systemNotificationService.getUnreadNotificationCount(userId);
        
        return ResponseEntity.ok(ResponseUtils.success("获取未读通知数量成功", Map.of("unreadCount", unreadCount)));
    }
} 