package com.im.imcommunicationsystem.message.repository;

import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话数据访问层接口
 * 定义会话相关的数据库操作
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 根据用户ID查询参与的会话
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND c.deleted = false " +
           "ORDER BY c.lastActiveAt DESC")
    Page<Conversation> findByParticipantUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID查询参与的会话列表
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND c.deleted = false")
    List<Conversation> findByParticipantUserId(@Param("userId") Long userId);

    /**
     * 查找两个用户之间的私聊会话
     * 
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @param conversationType 会话类型
     * @return 私聊会话
     */
    @Query("SELECT c FROM Conversation c " +
           "WHERE c.conversationType = :conversationType AND c.deleted = false " +
           "AND EXISTS (SELECT 1 FROM ConversationMember p1 WHERE p1.conversationId = c.id AND p1.userId = :userId1) " +
        "AND EXISTS (SELECT 1 FROM ConversationMember p2 WHERE p2.conversationId = c.id AND p2.userId = :userId2) " +
        "AND (SELECT COUNT(p) FROM ConversationMember p WHERE p.conversationId = c.id) = 2")
    Optional<Conversation> findPrivateConversation(@Param("userId1") Long userId1,
                                                  @Param("userId2") Long userId2,
                                                  @Param("conversationType") ConversationType conversationType);

    /**
     * 根据会话类型查询会话
     * 
     * @param conversationType 会话类型
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    Page<Conversation> findByConversationTypeAndDeletedFalseOrderByLastActiveAtDesc(
            ConversationType conversationType, Pageable pageable);

    /**
     * 根据创建者ID查询会话
     * 
     * @param createdBy 创建者ID
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    Page<Conversation> findByCreatedByAndDeletedFalseOrderByCreatedAtDesc(
            Long createdBy, Pageable pageable);

    /**
     * 更新会话最后活跃时间
     * 
     * @param conversationId 会话ID
     * @param lastActiveAt 最后活跃时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.lastActiveAt = :lastActiveAt WHERE c.id = :conversationId")
    int updateLastActiveTime(@Param("conversationId") Long conversationId,
                            @Param("lastActiveAt") LocalDateTime lastActiveAt);

    /**
     * 软删除会话
     * 
     * @param conversationId 会话ID
     * @param deletedAt 删除时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.deleted = true, c.deletedAt = :deletedAt WHERE c.id = :conversationId")
    int softDeleteConversation(@Param("conversationId") Long conversationId,
                              @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 根据名称搜索会话
     * 
     * @param name 会话名称
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 会话分页结果
     */
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND c.deleted = false " +
           "AND (c.name LIKE %:name% OR c.description LIKE %:name%) " +
           "ORDER BY c.lastActiveAt DESC")
    Page<Conversation> searchByName(@Param("name") String name,
                                   @Param("userId") Long userId,
                                   Pageable pageable);

    /**
     * 查询用户置顶的会话
     * 
     * @param userId 用户ID
     * @return 置顶会话列表
     */
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND p.isPinned = true AND c.deleted = false")
    List<Conversation> findPinnedConversations(@Param("userId") Long userId);

    /**
     * 查询用户归档的会话
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 归档会话分页结果
     */
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND p.isArchived = true AND c.deleted = false")
    Page<Conversation> findArchivedConversations(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计用户参与的会话数量
     * 
     * @param userId 用户ID
     * @return 会话数量
     */
    @Query("SELECT COUNT(DISTINCT c) FROM Conversation c JOIN c.participants p " +
           "WHERE p.userId = :userId AND c.deleted = false")
    Long countByParticipantUserId(@Param("userId") Long userId);

    /**
     * 查询需要清理的会话
     * 
     * @param beforeTime 时间点
     * @return 会话列表
     */
    @Query("SELECT c FROM Conversation c WHERE c.deleted = true AND c.deletedAt < :beforeTime")
    List<Conversation> findConversationsToCleanup(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根据关联的群组ID查询会话
     * 
     * @param groupId 群组ID
     * @return 会话
     */
    Conversation findByRelatedGroupId(Long groupId);
}