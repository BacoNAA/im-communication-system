package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.SystemNotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统通知状态仓库接口
 */
@Repository
public interface SystemNotificationStatusRepository extends JpaRepository<SystemNotificationStatus, Long> {

    /**
     * 根据通知ID和用户ID查找通知状态
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 通知状态
     */
    Optional<SystemNotificationStatus> findByNotificationIdAndUserId(Long notificationId, Long userId);

    /**
     * 根据用户ID查找所有通知状态
     *
     * @param userId 用户ID
     * @param pageable 分页信息
     * @return 通知状态列表
     */
    Page<SystemNotificationStatus> findByUserId(Long userId, Pageable pageable);

    /**
     * 查找用户的未读通知
     *
     * @param userId 用户ID
     * @param pageable 分页信息
     * @return 未读通知状态列表
     */
    Page<SystemNotificationStatus> findByUserIdAndIsReadFalse(Long userId, Pageable pageable);

    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 更新通知状态为已读
     *
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE SystemNotificationStatus s SET s.isRead = true, s.readAt = CURRENT_TIMESTAMP WHERE s.notificationId = :notificationId AND s.userId = :userId")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * 批量更新通知状态为已读
     *
     * @param notificationIds 通知ID列表
     * @param userId 用户ID
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE SystemNotificationStatus s SET s.isRead = true, s.readAt = CURRENT_TIMESTAMP WHERE s.notificationId IN :notificationIds AND s.userId = :userId")
    int markMultipleAsRead(@Param("notificationIds") List<Long> notificationIds, @Param("userId") Long userId);

    /**
     * 将用户所有通知标记为已读
     *
     * @param userId 用户ID
     * @return 更新记录数
     */
    @Modifying
    @Query("UPDATE SystemNotificationStatus s SET s.isRead = true, s.readAt = CURRENT_TIMESTAMP WHERE s.userId = :userId AND s.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
} 