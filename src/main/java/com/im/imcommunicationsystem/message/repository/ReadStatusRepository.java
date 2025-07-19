package com.im.imcommunicationsystem.message.repository;

import com.im.imcommunicationsystem.message.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 已读状态数据访问层接口
 * 定义已读状态相关的数据库操作
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-07-18
 */
@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    /**
     * 根据用户ID和会话ID查询已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 已读状态
     */
    Optional<ReadStatus> findByUserIdAndConversationId(Long userId, Long conversationId);

    /**
     * 根据用户ID查询所有已读状态
     * 
     * @param userId 用户ID
     * @return 已读状态列表
     */
    List<ReadStatus> findByUserId(Long userId);

    /**
     * 根据会话ID查询所有已读状态
     * 
     * @param conversationId 会话ID
     * @return 已读状态列表
     */
    List<ReadStatus> findByConversationId(Long conversationId);

    /**
     * 更新已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param lastReadMessageId 最后已读消息ID
     * @param updatedAt 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ReadStatus rs SET rs.lastReadMessageId = :lastReadMessageId, rs.updatedAt = :updatedAt " +
           "WHERE rs.userId = :userId AND rs.conversationId = :conversationId")
    int updateReadStatus(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 批量更新已读状态
     * 
     * @param userId 用户ID
     * @param conversationIds 会话ID列表
     * @param lastReadMessageId 最后已读消息ID
     * @param updatedAt 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ReadStatus rs SET rs.lastReadMessageId = :lastReadMessageId, rs.updatedAt = :updatedAt " +
           "WHERE rs.userId = :userId AND rs.conversationId IN :conversationIds")
    int updateReadStatusBatch(
            @Param("userId") Long userId,
            @Param("conversationIds") List<Long> conversationIds,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 删除用户在指定会话中的已读状态
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 删除的记录数
     */
    int deleteByUserIdAndConversationId(Long userId, Long conversationId);

    /**
     * 删除用户的所有已读状态
     * 
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteByUserId(Long userId);

    /**
     * 删除会话的所有已读状态
     * 
     * @param conversationId 会话ID
     * @return 删除的记录数
     */
    int deleteByConversationId(Long conversationId);
} 