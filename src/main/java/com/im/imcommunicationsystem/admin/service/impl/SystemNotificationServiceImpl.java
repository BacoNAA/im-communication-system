package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import com.im.imcommunicationsystem.admin.exception.AdminPermissionException;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.repository.SystemNotificationRepository;
import com.im.imcommunicationsystem.admin.service.SystemNotificationService;
import com.im.imcommunicationsystem.admin.utils.AdminPermissionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统通知服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationServiceImpl implements SystemNotificationService {

    private final SystemNotificationRepository systemNotificationRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;
    private final AdminPermissionUtils adminPermissionUtils;

    @Override
    @Transactional
    public SystemNotification createSystemNotification(Long adminId, SystemNotificationRequest request) {
        // 检查管理员权限
        if (!adminPermissionUtils.hasSystemNotificationPermission(adminId)) {
            throw new AdminPermissionException("管理员没有创建系统通知的权限");
        }

        // 创建系统通知
        SystemNotification notification = SystemNotification.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .targetType(request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty() ? 
                        SystemNotification.TargetType.specific_users : SystemNotification.TargetType.all)
                .targetUsers(request.getTargetUserIds() != null ? request.getTargetUserIds().toString() : null)
                .isPublished(false)
                .createdBy(adminId)
                .createdAt(LocalDateTime.now())
                .build();

        SystemNotification savedNotification = systemNotificationRepository.save(notification);

        // 记录操作日志
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(adminId)
                .operationType("CREATE_NOTIFICATION")
                .targetType("NOTIFICATION")
                .targetId(savedNotification.getId())
                .description("创建通知: " + request.getTitle())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        return savedNotification;
    }

    @Override
    @Transactional
    public SystemNotification updateSystemNotification(Long adminId, Long notificationId, SystemNotificationRequest request) {
        // 检查管理员权限
        if (!adminPermissionUtils.hasSystemNotificationPermission(adminId)) {
            throw new AdminPermissionException("管理员没有更新系统通知的权限");
        }

        // 查找通知
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + notificationId + " 的系统通知"));

        // 更新通知
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setTargetType(request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty() ? 
                SystemNotification.TargetType.specific_users : SystemNotification.TargetType.all);
        notification.setTargetUsers(request.getTargetUserIds() != null ? request.getTargetUserIds().toString() : null);

        SystemNotification updatedNotification = systemNotificationRepository.save(notification);

        // 记录操作日志
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(adminId)
                .operationType("UPDATE_NOTIFICATION")
                .targetType("NOTIFICATION")
                .targetId(updatedNotification.getId())
                .description("更新通知: " + request.getTitle())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        return updatedNotification;
    }

    @Override
    @Transactional
    public void deleteSystemNotification(Long adminId, Long notificationId) {
        // 检查管理员权限
        if (!adminPermissionUtils.hasSystemNotificationPermission(adminId)) {
            throw new AdminPermissionException("管理员没有删除系统通知的权限");
        }

        // 查找通知
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + notificationId + " 的系统通知"));

        // 删除前记录操作日志
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(adminId)
                .operationType("DELETE_NOTIFICATION")
                .targetType("NOTIFICATION")
                .targetId(notificationId)
                .description("删除通知: " + notification.getTitle())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        // 删除通知
        systemNotificationRepository.delete(notification);
    }

    @Override
    public SystemNotification getSystemNotificationById(Long notificationId) {
        return systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + notificationId + " 的系统通知"));
    }

    @Override
    public Page<SystemNotification> getSystemNotificationsWithPagination(Pageable pageable, String type) {
        if (type != null) {
            return systemNotificationRepository.findByType(type, pageable);
        } else {
            return systemNotificationRepository.findAll(pageable);
        }
    }

    @Override
    public Page<SystemNotification> getActiveSystemNotifications(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return systemNotificationRepository.findActiveNotifications(now, pageable);
    }
    
    @Override
    @Transactional
    public SystemNotification publishSystemNotification(Long adminId, Long notificationId) {
        // 检查管理员权限
        if (!adminPermissionUtils.hasSystemNotificationPermission(adminId)) {
            throw new AdminPermissionException("管理员没有发布系统通知的权限");
        }

        // 查找通知
        SystemNotification notification = systemNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + notificationId + " 的系统通知"));

        // 更新通知状态为已发布
        notification.setIsPublished(true);
        notification.setPublishedAt(LocalDateTime.now());

        SystemNotification publishedNotification = systemNotificationRepository.save(notification);

        // 记录操作日志
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(adminId)
                .operationType("PUBLISH_NOTIFICATION")
                .targetType("NOTIFICATION")
                .targetId(publishedNotification.getId())
                .description("发布通知: " + notification.getTitle())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        return publishedNotification;
    }
} 