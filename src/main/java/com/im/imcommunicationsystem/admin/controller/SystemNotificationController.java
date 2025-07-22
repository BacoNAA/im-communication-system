package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import com.im.imcommunicationsystem.admin.enums.OperationType;
import com.im.imcommunicationsystem.admin.enums.TargetType;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
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

import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 系统通知控制器
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationController {

    private final SystemNotificationService systemNotificationService;
    private final AdminOperationLogRepository adminOperationLogRepository;
    
    /**
     * 记录管理员操作日志
     * 
     * @param adminId 管理员ID
     * @param operationType 操作类型
     * @param targetId 目标ID
     * @param description 描述
     */
    private void logAdminOperation(Long adminId, OperationType operationType, Long targetId, String description) {
        if (adminId == null) {
            log.warn("记录管理员操作日志失败: adminId为空");
            return;
        }
        
        try {
            AdminOperationLog operationLog = new AdminOperationLog();
            operationLog.setAdminId(adminId);
            operationLog.setOperationType(operationType != null ? operationType : OperationType.OTHER);
            operationLog.setTargetType(TargetType.NOTIFICATION);
            operationLog.setTargetId(targetId);
            // 确保描述不为空
            operationLog.setDescription(description != null ? description : "无描述");
            operationLog.setCreatedAt(LocalDateTime.now());
            
            AdminOperationLog savedLog = adminOperationLogRepository.save(operationLog);
            log.debug("记录管理员操作日志成功: id={}, adminId={}, operationType={}, targetId={}", 
                    savedLog.getId(), adminId, operationType, targetId);
        } catch (Exception e) {
            log.error("记录管理员操作日志失败: adminId={}, operationType={}, targetId={}, error={}", 
                    adminId, operationType, targetId, e.getMessage(), e);
            // 不影响主流程
        }
    }

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
            @RequestParam(required = false) Long adminId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("创建新通知，标题: {}", request.getTitle());
        
        // 优先使用请求参数中的adminId，如果为空则从认证用户中提取
        Long effectiveAdminId = adminId;
        if (effectiveAdminId == null && userDetails != null) {
            effectiveAdminId = Long.valueOf(userDetails.getUsername());
        }
        
        // 如果仍然无法获取adminId，返回错误
        if (effectiveAdminId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("无法获取管理员ID"));
        }
        
        try {
            // 执行创建操作
            SystemNotification notification = systemNotificationService.createSystemNotification(effectiveAdminId, request);
            
            if (notification != null) {
                // 记录操作日志，使用try-catch包裹，确保日志错误不影响主流程
                try {
                    logAdminOperation(
                        effectiveAdminId,
                        OperationType.CREATE_NOTIFICATION, 
                        notification.getId(),
                        String.format("创建系统通知: %s (ID: %d)", notification.getTitle(), notification.getId())
                    );
                } catch (Exception e) {
                    log.error("记录创建通知日志失败: {}", e.getMessage(), e);
                    // 不抛出异常，继续流程
                }
        
        return ResponseEntity.ok(ResponseUtils.success("通知创建成功", notification));
            } else {
                // 创建返回null的异常情况
                log.error("创建通知返回null");
                return ResponseEntity.badRequest().body(ResponseUtils.error("创建通知失败，无法获取通知信息"));
            }
        } catch (Exception e) {
            // 捕获所有可能的异常
            log.error("创建通知时发生异常: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseUtils.error("创建通知失败: " + e.getMessage()));
        }
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
            @RequestParam(required = false) Long adminId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("更新通知，通知ID: {}，标题: {}", notificationId, request.getTitle());
        
        // 优先使用请求参数中的adminId，如果为空则从认证用户中提取
        Long effectiveAdminId = adminId;
        if (effectiveAdminId == null && userDetails != null) {
            effectiveAdminId = Long.valueOf(userDetails.getUsername());
        }
        
        // 如果仍然无法获取adminId，返回错误
        if (effectiveAdminId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("无法获取管理员ID"));
        }
        
        try {
            // 先获取原通知信息，用于日志记录和错误处理
            SystemNotification existingNotification = null;
            try {
                existingNotification = systemNotificationService.getSystemNotificationById(notificationId);
                log.debug("获取到原通知信息: id={}, title={}", existingNotification.getId(), existingNotification.getTitle());
            } catch (Exception e) {
                log.warn("获取原通知信息失败: {}", e.getMessage());
                // 继续执行，即使获取原通知信息失败
            }
            
            // 执行更新操作
            SystemNotification notification = systemNotificationService.updateSystemNotification(effectiveAdminId, notificationId, request);
            
            if (notification != null) {
                // 记录操作日志，使用try-catch包裹，确保日志错误不影响主流程
                try {
                    logAdminOperation(
                        effectiveAdminId,
                        OperationType.UPDATE_NOTIFICATION, 
                        notification.getId(),
                        String.format("更新系统通知: %s (ID: %d)", notification.getTitle(), notification.getId())
                    );
                } catch (Exception e) {
                    log.error("记录更新通知日志失败: {}", e.getMessage(), e);
                    // 不抛出异常，继续流程
                }
        
        return ResponseEntity.ok(ResponseUtils.success("通知更新成功", notification));
            } else {
                // 更新返回null的异常情况
                log.error("更新通知返回null: notificationId={}", notificationId);
                return ResponseEntity.badRequest().body(ResponseUtils.error("更新通知失败，无法获取更新后的通知信息"));
            }
        } catch (Exception e) {
            // 捕获所有可能的异常
            log.error("更新通知时发生异常: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseUtils.error("更新通知失败: " + e.getMessage()));
        }
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
            @RequestParam(required = false) Long adminId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("删除通知，通知ID: {}", notificationId);
        
        // 优先使用请求参数中的adminId，如果为空则从认证用户中提取
        Long effectiveAdminId = adminId;
        if (effectiveAdminId == null && userDetails != null) {
            effectiveAdminId = Long.valueOf(userDetails.getUsername());
        }
        
        // 如果仍然无法获取adminId，返回错误
        if (effectiveAdminId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("无法获取管理员ID"));
        }
        
        // 获取通知信息用于日志记录
        SystemNotification notification = null;
        try {
            notification = systemNotificationService.getSystemNotificationById(notificationId);
        } catch (Exception e) {
            log.warn("获取要删除的通知信息失败: {}", e.getMessage());
            // 继续处理删除操作，即使获取通知信息失败
        }
        
        // 执行删除操作
        systemNotificationService.deleteSystemNotification(effectiveAdminId, notificationId);
        
        // 记录操作日志
        String notificationTitle = notification != null ? notification.getTitle() : "未知通知";
        logAdminOperation(
            effectiveAdminId,
            OperationType.DELETE_NOTIFICATION, 
            notificationId,
            String.format("删除系统通知: %s (ID: %d)", notificationTitle, notificationId)
        );
        
        return ResponseEntity.ok(ResponseUtils.success("通知删除成功"));
    }

    /**
     * 发布系统通知
     *
     * @param notificationId 通知ID
     * @param request 包含管理员ID的请求体
     * @param userDetails 管理员用户
     * @return 包含发布后通知的响应
     */
    @PostMapping("/{notificationId}/publish")
    public ResponseEntity<ResponseUtils.ApiResponse<SystemNotification>> publishNotification(
            @PathVariable Long notificationId,
            @RequestBody(required = false) com.im.imcommunicationsystem.admin.dto.request.AdminIdRequest request,
            @RequestParam(required = false) Long adminId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("发布系统通知，通知ID: {}", notificationId);
        
        // 优先使用请求体中的adminId，其次使用请求参数中的adminId，最后从认证用户中提取
        Long effectiveAdminId = null;
        
        // 检查请求体中是否有adminId
        if (request != null && request.getAdminId() != null && !request.getAdminId().isEmpty()) {
            try {
                effectiveAdminId = Long.valueOf(request.getAdminId());
                log.info("使用请求体中的管理员ID: {}", effectiveAdminId);
            } catch (NumberFormatException e) {
                log.error("请求体中的管理员ID格式错误: {}", request.getAdminId(), e);
            }
        }
        
        // 如果请求体中没有有效的adminId，则使用URL参数
        if (effectiveAdminId == null && adminId != null) {
            effectiveAdminId = adminId;
            log.info("使用URL参数中的管理员ID: {}", effectiveAdminId);
        }
        
        // 如果请求体和URL参数中都没有，则从认证用户中提取
        if (effectiveAdminId == null && userDetails != null) {
            try {
                effectiveAdminId = Long.valueOf(userDetails.getUsername());
                log.info("使用认证用户中的管理员ID: {}", effectiveAdminId);
            } catch (NumberFormatException e) {
                log.error("认证用户中的用户名无法转换为Long: {}", userDetails.getUsername(), e);
            }
        }
        
        // 如果仍然无法获取adminId，返回错误
        if (effectiveAdminId == null) {
            return ResponseEntity.badRequest().body(ResponseUtils.error("无法获取管理员ID"));
        }
        
        try {
            // 先获取原通知信息，用于日志记录和错误处理
            SystemNotification existingNotification = null;
            try {
                existingNotification = systemNotificationService.getSystemNotificationById(notificationId);
                log.debug("获取到通知信息: id={}, title={}", existingNotification.getId(), existingNotification.getTitle());
            } catch (Exception e) {
                log.warn("获取通知信息失败: {}", e.getMessage());
                // 继续执行，即使获取原通知信息失败
            }
            
            // 执行发布操作
            SystemNotification notification = systemNotificationService.publishSystemNotification(effectiveAdminId, notificationId);
            
            if (notification != null) {
                // 记录操作日志，使用try-catch包裹，确保日志错误不影响主流程
                try {
                    logAdminOperation(
                        effectiveAdminId,
                        OperationType.PUBLISH_NOTIFICATION, 
                        notification.getId(),
                        String.format("发布系统通知: %s (ID: %d)", notification.getTitle(), notification.getId())
                    );
                } catch (Exception e) {
                    log.error("记录发布通知日志失败: {}", e.getMessage(), e);
                    // 不抛出异常，继续流程
                }
                
                return ResponseEntity.ok(ResponseUtils.success("通知发布成功", notification));
            } else {
                // 发布返回null的异常情况
                log.error("发布通知返回null: notificationId={}", notificationId);
                return ResponseEntity.badRequest().body(ResponseUtils.error("发布通知失败，无法获取通知信息"));
            }
        } catch (Exception e) {
            // 捕获所有可能的异常
            log.error("发布通知时发生异常: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseUtils.error("发布通知失败: " + e.getMessage()));
        }
    }
} 