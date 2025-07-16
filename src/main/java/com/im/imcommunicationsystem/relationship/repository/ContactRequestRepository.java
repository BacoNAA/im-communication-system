package com.im.imcommunicationsystem.relationship.repository;

import com.im.imcommunicationsystem.relationship.entity.ContactRequest;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 好友请求数据访问接口
 * 提供好友请求的CRUD操作
 */
@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    /**
     * 查找待处理的好友请求（按接收者）
     * @param recipientId 接收者ID
     * @return 待处理的好友请求列表
     */
    List<ContactRequest> findByRecipientIdAndStatus(Long recipientId, ContactRequestStatus status);

    /**
     * 查找特定的好友请求
     * @param requesterId 请求者ID
     * @param recipientId 接收者ID
     * @return 好友请求
     */
    Optional<ContactRequest> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);

    /**
     * 查找特定状态的好友请求
     * @param requesterId 请求者ID
     * @param recipientId 接收者ID
     * @param status 请求状态
     * @return 好友请求
     */
    Optional<ContactRequest> findByRequesterIdAndRecipientIdAndStatus(Long requesterId, Long recipientId, ContactRequestStatus status);

    /**
     * 查找已发送的好友请求（按请求者）
     * @param requesterId 请求者ID
     * @return 已发送的好友请求列表
     */
    List<ContactRequest> findByRequesterId(Long requesterId);

    /**
     * 查找已发送的特定状态好友请求
     * @param requesterId 请求者ID
     * @param status 请求状态
     * @return 好友请求列表
     */
    List<ContactRequest> findByRequesterIdAndStatus(Long requesterId, ContactRequestStatus status);

    /**
     * 查找接收到的好友请求
     * @param recipientId 接收者ID
     * @return 接收到的好友请求列表
     */
    List<ContactRequest> findByRecipientId(Long recipientId);

    /**
     * 检查是否存在待处理的好友请求
     * @param requesterId 请求者ID
     * @param recipientId 接收者ID
     * @return 是否存在待处理请求
     */
    boolean existsByRequesterIdAndRecipientIdAndStatus(Long requesterId, Long recipientId, ContactRequestStatus status);

    /**
     * 删除过期的好友请求
     * @param expireTime 过期时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactRequest cr WHERE cr.status = :status AND cr.createdAt < :expireTime")
    int deleteExpiredRequests(@Param("expireTime") LocalDateTime expireTime, @Param("status") ContactRequestStatus status);

    /**
     * 统计用户收到的待处理请求数量
     * @param recipientId 接收者ID
     * @return 待处理请求数量
     */
    long countByRecipientIdAndStatus(Long recipientId, ContactRequestStatus status);

    /**
     * 统计用户发送的待处理请求数量
     * @param requesterId 请求者ID
     * @return 待处理请求数量
     */
    long countByRequesterIdAndStatus(Long requesterId, ContactRequestStatus status);

    /**
     * 查找用户相关的所有好友请求（作为请求者或接收者）
     * @param userId 用户ID
     * @return 相关的好友请求列表
     */
    @Query("SELECT cr FROM ContactRequest cr WHERE cr.requesterId = :userId OR cr.recipientId = :userId")
    List<ContactRequest> findAllRelatedRequests(@Param("userId") Long userId);

    /**
     * 查找最近的好友请求
     * @param recipientId 接收者ID
     * @param limit 限制数量
     * @return 最近的好友请求列表
     */
    @Query("SELECT cr FROM ContactRequest cr WHERE cr.recipientId = :recipientId " +
           "ORDER BY cr.createdAt DESC LIMIT :limit")
    List<ContactRequest> findRecentRequests(@Param("recipientId") Long recipientId, @Param("limit") int limit);

    /**
     * 删除指定时间之前的特定状态请求
     * @param dateTime 时间点
     * @param status 请求状态
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactRequest cr WHERE cr.createdAt < :dateTime AND cr.status = :status")
    int deleteByCreatedAtBeforeAndStatus(@Param("dateTime") LocalDateTime dateTime, @Param("status") ContactRequestStatus status);

    /**
     * 查找用户最近的好友请求（作为请求者或接收者）
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近的好友请求列表
     */
    @Query("SELECT cr FROM ContactRequest cr WHERE cr.requesterId = :userId OR cr.recipientId = :userId " +
           "ORDER BY cr.createdAt DESC LIMIT :limit")
    List<ContactRequest> findRecentRequestsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查找接收到的好友请求（按创建时间倒序）
     * @param recipientId 接收者ID
     * @return 接收到的好友请求列表
     */
    List<ContactRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * 查找接收到的特定状态好友请求（按创建时间倒序）
     * @param recipientId 接收者ID
     * @param status 请求状态
     * @return 好友请求列表
     */
    List<ContactRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, ContactRequestStatus status);

    /**
     * 查找发送的好友请求（按创建时间倒序）
     * @param requesterId 请求者ID
     * @return 发送的好友请求列表
     */
    List<ContactRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    /**
     * 查找发送的特定状态好友请求（按创建时间倒序）
     * @param requesterId 请求者ID
     * @param status 请求状态
     * @return 好友请求列表
     */
    List<ContactRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, ContactRequestStatus status);
    
    /**
     * 统计用户收到的总请求数量
     * @param recipientId 接收者ID
     * @return 总请求数量
     */
    long countByRecipientId(Long recipientId);
    
    /**
     * 统计用户发送的总请求数量
     * @param requesterId 请求者ID
     * @return 总请求数量
     */
    long countByRequesterId(Long requesterId);
    
    /**
     * 统计用户在指定时间范围内收到的请求数量
     * @param recipientId 接收者ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求数量
     */
    long countByRecipientIdAndCreatedAtBetween(Long recipientId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计用户在指定时间范围内发送的请求数量
     * @param requesterId 请求者ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求数量
     */
    long countByRequesterIdAndCreatedAtBetween(Long requesterId, LocalDateTime startTime, LocalDateTime endTime);
}