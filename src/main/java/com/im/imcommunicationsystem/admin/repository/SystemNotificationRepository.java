package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统通知仓库接口
 */
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    
    /**
     * 根据类型查找通知
     * 
     * @param type 通知类型
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<SystemNotification> findByType(String type, Pageable pageable);
    
    /**
     * 查找当前生效的通知
     * 
     * @param now 当前时间
     * @param pageable 分页信息
     * @return 当前生效的通知分页结果
     */
    @Query("SELECT n FROM SystemNotification n WHERE n.isPublished = true " +
           "ORDER BY n.createdAt DESC")
    Page<SystemNotification> findActiveNotifications(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * 根据创建者查找通知
     * 
     * @param createdBy 创建者ID
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<SystemNotification> findByCreatedBy(Long createdBy, Pageable pageable);
    
    /**
     * 查找已发布的通知
     * 
     * @param pageable 分页信息
     * @return 已发布通知分页结果
     */
    Page<SystemNotification> findByIsPublishedTrue(Pageable pageable);
} 