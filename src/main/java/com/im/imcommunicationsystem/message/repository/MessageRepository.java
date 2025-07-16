package com.im.imcommunicationsystem.message.repository;

import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.enums.MessageType;
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
 * 消息数据访问层接口
 * 定义消息相关的数据库操作
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据会话ID分页查询消息
     * 
     * @param conversationId 会话ID
     * @param pageable 分页参数
     * @return 消息分页结果
     */
    Page<Message> findByConversationIdAndStatusNotOrderByCreatedAtDesc(
            Long conversationId, MessageStatus status, Pageable pageable);

    /**
     * 根据会话ID和消息ID之前查询消息
     * 
     * @param conversationId 会话ID
     * @param beforeMessageId 消息ID
     * @param status 排除的状态
     * @param pageable 分页参数
     * @return 消息分页结果
     */
    Page<Message> findByConversationIdAndIdLessThanAndStatusNotOrderByCreatedAtDesc(
            Long conversationId, Long beforeMessageId, MessageStatus status, Pageable pageable);

    /**
     * 查询会话中的最后一条消息
     * 
     * @param conversationId 会话ID
     * @param status 排除的状态
     * @return 最后一条消息
     */
    Optional<Message> findFirstByConversationIdAndStatusNotOrderByCreatedAtDesc(
            Long conversationId, MessageStatus status);

    /**
     * 统计会话中未读消息数量
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param createdAfter 创建时间之后
     * @param status 排除的状态
     * @return 未读消息数量
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId AND m.createdAt > :createdAfter AND m.status != :status")
    Long countUnreadMessages(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId,
                            @Param("createdAfter") LocalDateTime createdAfter,
                            @Param("status") MessageStatus status);

    /**
     * 根据发送者ID查询消息
     * 
     * @param senderId 发送者ID
     * @param pageable 分页参数
     * @return 消息分页结果
     */
    Page<Message> findBySenderIdAndStatusNotOrderByCreatedAtDesc(
            Long senderId, MessageStatus status, Pageable pageable);

    /**
     * 根据消息类型查询消息
     * 
     * @param conversationId 会话ID
     * @param messageType 消息类型
     * @param status 排除的状态
     * @param pageable 分页参数
     * @return 消息分页结果
     */
    Page<Message> findByConversationIdAndMessageTypeAndStatusNotOrderByCreatedAtDesc(
            Long conversationId, MessageType messageType, MessageStatus status, Pageable pageable);

    /**
     * 批量更新消息状态
     * 
     * @param messageIds 消息ID列表
     * @param status 新状态
     * @param updatedAt 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Message m SET m.status = :status, m.updatedAt = :updatedAt WHERE m.id IN :messageIds")
    int updateMessageStatus(@Param("messageIds") List<Long> messageIds,
                           @Param("status") MessageStatus status,
                           @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据时间范围查询消息
     * 
     * @param conversationId 会话ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 排除的状态
     * @return 消息列表
     */
    List<Message> findByConversationIdAndCreatedAtBetweenAndStatusNotOrderByCreatedAtAsc(
            Long conversationId, LocalDateTime startTime, LocalDateTime endTime, MessageStatus status);

    /**
     * 查询需要索引的消息
     * 
     * @param limit 限制数量
     * @return 消息列表
     */
    @Query("SELECT m FROM Message m WHERE m.indexed = false AND m.status != :status ORDER BY m.createdAt ASC")
    List<Message> findUnindexedMessages(@Param("status") MessageStatus status, Pageable pageable);

    /**
     * 标记消息为已索引
     * 
     * @param messageIds 消息ID列表
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Message m SET m.indexed = true WHERE m.id IN :messageIds")
    int markAsIndexed(@Param("messageIds") List<Long> messageIds);

    /**
     * 根据引用消息ID查询消息
     * 
     * @param replyToMessageId 引用消息ID
     * @return 消息列表
     */
    List<Message> findByReplyToMessageId(Long replyToMessageId);

    /**
     * 查询转发的消息
     * 
     * @param originalMessageId 原始消息ID
     * @return 消息列表
     */
    List<Message> findByOriginalMessageId(Long originalMessageId);
}