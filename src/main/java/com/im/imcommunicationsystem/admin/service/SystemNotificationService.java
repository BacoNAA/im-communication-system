package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.SystemNotificationRequest;
import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationDetailResponse;
import com.im.imcommunicationsystem.admin.dto.response.SystemNotificationResponse;
import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
    
    /**
     * 为所有用户创建通知状态记录
     *
     * @param notificationId 通知ID
     */
    void createNotificationStatusForAllUsers(Long notificationId);
    
    /**
     * 为特定用户创建通知状态记录
     *
     * @param notificationId 通知ID
     * @param userIds 用户ID列表
     */
    void createNotificationStatusForUsers(Long notificationId, List<Long> userIds);
    
    /**
     * 通过WebSocket推送通知给在线用户
     *
     * @param notification 系统通知
     */
    void pushNotificationToOnlineUsers(SystemNotification notification);
    
    /**
     * 获取用户的通知列表
     *
     * @param userId 用户ID
     * @param pageable 分页信息
     * @param unreadOnly 是否只获取未读通知
     * @return 通知列表
     */
    Page<SystemNotificationResponse> getUserNotifications(Long userId, Pageable pageable, boolean unreadOnly);
    
    /**
     * 获取用户的通知详情
     *
     * @param userId 用户ID
     * @param notificationId 通知ID
     * @return 通知详情
     */
    SystemNotificationDetailResponse getUserNotificationDetail(Long userId, Long notificationId);
    
    /**
     * 标记通知为已读
     *
     * @param userId 用户ID
     * @param notificationId 通知ID
     * @return 是否成功
     */
    boolean markNotificationAsRead(Long userId, Long notificationId);
    
    /**
     * 标记多个通知为已读
     *
     * @param userId 用户ID
     * @param notificationIds 通知ID列表
     * @return 更新的记录数
     */
    int markMultipleNotificationsAsRead(Long userId, List<Long> notificationIds);
    
    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     * @return 更新的记录数
     */
    int markAllNotificationsAsRead(Long userId);
    
    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long getUnreadNotificationCount(Long userId);
} 