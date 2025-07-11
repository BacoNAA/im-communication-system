package com.im.imcommunicationsystem.relationship.repository;

import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 联系人数据访问接口
 * 提供联系人关系的CRUD操作
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, ContactId> {

    /**
     * 查找特定联系人关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 联系人关系
     */
    Optional<Contact> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 查找用户的所有联系人
     * @param userId 用户ID
     * @return 联系人列表
     */
    List<Contact> findByUserId(Long userId);

    /**
     * 查找用户的所有联系人（排除被屏蔽的）
     * @param userId 用户ID
     * @return 联系人列表
     */
    List<Contact> findByUserIdAndIsBlockedFalse(Long userId);

    /**
     * 查找被屏蔽的联系人
     * @param userId 用户ID
     * @return 被屏蔽的联系人列表
     */
    List<Contact> findByUserIdAndIsBlockedTrue(Long userId);

    /**
     * 检查联系人关系是否存在
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否存在关系
     */
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 检查双向好友关系是否存在
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 是否存在双向好友关系
     */
    @Query("SELECT COUNT(c) = 2 FROM Contact c WHERE " +
           "(c.userId = :userId1 AND c.friendId = :userId2) OR " +
           "(c.userId = :userId2 AND c.friendId = :userId1)")
    boolean existsMutualFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 根据好友ID查找所有相关联系人关系
     * @param friendId 好友ID
     * @return 联系人关系列表
     */
    List<Contact> findByFriendId(Long friendId);

    /**
     * 统计用户的好友数量（排除被屏蔽的）
     * @param userId 用户ID
     * @return 好友数量
     */
    long countByUserIdAndIsBlockedFalse(Long userId);

    /**
     * 根据备注名搜索联系人
     * @param userId 用户ID
     * @param alias 备注名（模糊匹配）
     * @return 联系人列表
     */
    @Query("SELECT c FROM Contact c WHERE c.userId = :userId AND c.alias LIKE %:alias%")
    List<Contact> findByUserIdAndAliasContaining(@Param("userId") Long userId, @Param("alias") String alias);
}