package com.im.imcommunicationsystem.message.repository;

import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话参与者数据访问层接口
 * 定义会话参与者相关的数据库操作
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    /**
     * 根据会话ID查询参与者
     * 
     * @param conversationId 会话ID
     * @return 参与者列表
     */
    List<ConversationMember> findByConversationId(Long conversationId);

    /**
     * 根据用户ID查询参与的会话
     * 
     * @param userId 用户ID
     * @return 参与者列表
     */
    List<ConversationMember> findByUserId(Long userId);

    /**
     * 查询会话中的特定用户
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 参与者信息
     */
    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 检查用户是否参与会话
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 统计会话参与者数量
     * 
     * @param conversationId 会话ID
     * @return 参与者数量
     */
    Long countByConversationId(Long conversationId);





    /**
     * 更新最后已读消息ID
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param lastReadMessageId 最后已读消息ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ConversationMember p SET p.lastReadMessageId = :lastReadMessageId " +
           "WHERE p.conversationId = :conversationId AND p.userId = :userId")
    int updateLastReadMessageId(@Param("conversationId") Long conversationId,
                               @Param("userId") Long userId,
                               @Param("lastReadMessageId") Long lastReadMessageId);

    /**
     * 更新置顶状态
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isPinned 是否置顶
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ConversationMember p SET p.isPinned = :isPinned " +
           "WHERE p.conversationId = :conversationId AND p.userId = :userId")
    int updatePinnedStatus(@Param("conversationId") Long conversationId,
                          @Param("userId") Long userId,
                          @Param("isPinned") Boolean isPinned);

    /**
     * 更新归档状态
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isArchived 是否归档
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ConversationMember p SET p.isArchived = :isArchived " +
           "WHERE p.conversationId = :conversationId AND p.userId = :userId")
    int updateArchivedStatus(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId,
                            @Param("isArchived") Boolean isArchived);



    /**
     * 更新草稿内容
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param draft 草稿内容
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE ConversationMember p SET p.draft = :draft " +
           "WHERE p.conversationId = :conversationId AND p.userId = :userId")
    int updateDraft(@Param("conversationId") Long conversationId,
                   @Param("userId") Long userId,
                   @Param("draft") String draft);

    /**
     * 删除会话参与者
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 批量删除会话的所有参与者
     * 
     * @param conversationId 会话ID
     * @return 删除的记录数
     */
    int deleteByConversationId(Long conversationId);

    /**
     * 查询用户置顶的会话参与者记录
     * 
     * @param userId 用户ID
     * @return 置顶的参与者记录列表
     */
    List<ConversationMember> findByUserIdAndIsPinnedTrue(Long userId);

    /**
     * 查询用户归档的会话参与者记录
     * 
     * @param userId 用户ID
     * @return 归档的参与者记录列表
     */
    List<ConversationMember> findByUserIdAndIsArchivedTrue(Long userId);

    /**
     * 获取用户参与的所有会话ID列表
     * 
     * @param userId 用户ID
     * @return 会话ID列表
     */
    @Query("SELECT cm.conversationId FROM ConversationMember cm WHERE cm.userId = :userId")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);
}