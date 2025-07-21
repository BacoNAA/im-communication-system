package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import com.im.imcommunicationsystem.admin.service.SystemNotificationService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知控制器
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationController {

    private final SystemNotificationService systemNotificationService;

    /**
     * 获取系统通知列表（分页）
     *
     * @param pageable 分页信息
     * @param type 通知类型过滤
     * @return 包含通知列表的响应
     */
    @GetMapping
    public ResponseEntity<ResponseUtils.ApiResponse<Page<SystemNotification>>> getNotifications(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String type) {
        
        log.info("获取通知列表，类型过滤: {}", type);
        Page<SystemNotification> notifications = systemNotificationService.getSystemNotificationsWithPagination(pageable, type);
        
        return ResponseEntity.ok(ResponseUtils.success("通知列表获取成功", notifications));
    }

    /**
     * 获取活跃的系统通知
     *
     * @param pageable 分页信息
     * @return 包含活跃通知的响应
     */
    @GetMapping("/active")
    public ResponseEntity<ResponseUtils.ApiResponse<Page<SystemNotification>>> getActiveNotifications(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("获取活跃通知");
        Page<SystemNotification> notifications = systemNotificationService.getActiveSystemNotifications(pageable);
        
        return ResponseEntity.ok(ResponseUtils.success("活跃通知获取成功", notifications));
    }

    /**
     * 获取通知详情
     *
     * @param notificationId 通知ID
     * @return 包含通知详情的响应
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<ResponseUtils.ApiResponse<SystemNotification>> getNotificationDetails(
            @PathVariable Long notificationId) {
        
        log.info("获取通知详情，通知ID: {}", notificationId);
        SystemNotification notification = systemNotificationService.getSystemNotificationById(notificationId);
        
        return ResponseEntity.ok(ResponseUtils.success("通知详情获取成功", notification));
    }

    /**
     * 创建新通知
     *
     * @param request 通知请求
     * @param userDetails 已认证的管理员用户
     * @return 包含创建的通知的响应
     */
    @PostMapping
    public ResponseEntity<ResponseUtils.ApiResponse<SystemNotification>> createNotification(
            @Valid @RequestBody SystemNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("创建新通知，标题: {}", request.getTitle());
        
        // 从已认证用户中提取管理员ID
        Long adminId = Long.valueOf(userDetails.getUsername());
        
        SystemNotification notification = systemNotificationService.createSystemNotification(adminId, request);
        
        return ResponseEntity.ok(ResponseUtils.success("通知创建成功", notification));
    }

    /**
     * 更新已有通知
     *
     * @param notificationId 通知ID
     * @param request 通知请求
     * @param userDetails 已认证的管理员用户
     * @return 包含更新后通知的响应
     */
    @PutMapping("/{notificationId}")
    public ResponseEntity<ResponseUtils.ApiResponse<SystemNotification>> updateNotification(
            @PathVariable Long notificationId,
            @Valid @RequestBody SystemNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("更新通知，通知ID: {}，标题: {}", notificationId, request.getTitle());
        
        // 从已认证用户中提取管理员ID
        Long adminId = Long.valueOf(userDetails.getUsername());
        
        SystemNotification notification = systemNotificationService.updateSystemNotification(adminId, notificationId, request);
        
        return ResponseEntity.ok(ResponseUtils.success("通知更新成功", notification));
    }

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @param userDetails 已认证的管理员用户
     * @return 包含成功消息的响应
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ResponseUtils.ApiResponse<String>> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("删除通知，通知ID: {}", notificationId);
        
        // 从已认证用户中提取管理员ID
        Long adminId = Long.valueOf(userDetails.getUsername());
        
        systemNotificationService.deleteSystemNotification(adminId, notificationId);
        
        return ResponseEntity.ok(ResponseUtils.success("通知删除成功"));
    }
} 