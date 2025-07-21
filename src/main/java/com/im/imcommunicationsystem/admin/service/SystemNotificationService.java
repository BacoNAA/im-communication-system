package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 系统通知服务接口
 */
public interface SystemNotificationService {

    /**
     * 创建新的系统通知
     *
     * @param adminId 管理员ID
     * @param request 系统通知请求
     * @return 创建的系统通知
     */
    SystemNotification createSystemNotification(Long adminId, SystemNotificationRequest request);

    /**
     * 更新现有的系统通知
     *
     * @param adminId 管理员ID
     * @param notificationId 通知ID
     * @param request 系统通知请求
     * @return 更新后的系统通知
     */
    SystemNotification updateSystemNotification(Long adminId, Long notificationId, SystemNotificationRequest request);

    /**
     * 删除系统通知
     *
     * @param adminId 管理员ID
     * @param notificationId 通知ID
     */
    void deleteSystemNotification(Long adminId, Long notificationId);

    /**
     * 根据ID获取系统通知
     *
     * @param notificationId 通知ID
     * @return 系统通知
     */
    SystemNotification getSystemNotificationById(Long notificationId);

    /**
     * 获取系统通知列表（分页）
     *
     * @param pageable 分页信息
     * @param type 通知类型过滤（可选）
     * @return 系统通知分页结果
     */
    Page<SystemNotification> getSystemNotificationsWithPagination(Pageable pageable, String type);

    /**
     * 获取活跃的系统通知
     *
     * @param pageable 分页信息
     * @return 活跃系统通知分页结果
     */
    Page<SystemNotification> getActiveSystemNotifications(Pageable pageable);
    
    /**
     * 发布系统通知
     *
     * @param adminId 管理员ID
     * @param notificationId 通知ID
     * @return 发布后的系统通知
     */
    SystemNotification publishSystemNotification(Long adminId, Long notificationId);
} 