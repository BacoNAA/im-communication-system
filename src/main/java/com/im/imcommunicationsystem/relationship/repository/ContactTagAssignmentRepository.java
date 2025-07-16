package com.im.imcommunicationsystem.relationship.repository;

import com.im.imcommunicationsystem.relationship.entity.ContactTagAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 联系人标签分配数据访问接口
 * 提供联系人标签分配的CRUD操作
 */
@Repository
public interface ContactTagAssignmentRepository extends JpaRepository<ContactTagAssignment, Long> {

    /**
     * 查找用户为指定好友分配的所有标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 标签分配列表
     */
    List<ContactTagAssignment> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 查找使用指定标签的所有联系人
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 标签分配列表
     */
    List<ContactTagAssignment> findByUserIdAndTagId(Long userId, Long tagId);

    /**
     * 查找用户的所有标签分配
     * @param userId 用户ID
     * @return 标签分配列表
     */
    List<ContactTagAssignment> findByUserId(Long userId);

    /**
     * 检查是否已经为好友分配了指定标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagId 标签ID
     * @return 是否存在
     */
    boolean existsByUserIdAndFriendIdAndTagId(Long userId, Long friendId, Long tagId);

    /**
     * 查找特定的标签分配记录
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagId 标签ID
     * @return 标签分配记录
     */
    Optional<ContactTagAssignment> findByUserIdAndFriendIdAndTagId(Long userId, Long friendId, Long tagId);

    /**
     * 删除用户为指定好友分配的所有标签
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactTagAssignment cta WHERE cta.userId = :userId AND cta.friendId = :friendId")
    int deleteByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 删除指定标签的所有分配
     * @param tagId 标签ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactTagAssignment cta WHERE cta.tagId = :tagId")
    int deleteByTagId(@Param("tagId") Long tagId);

    /**
     * 删除用户的所有标签分配
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactTagAssignment cta WHERE cta.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 统计标签的使用次数
     * @param tagId 标签ID
     * @return 使用次数
     */
    long countByTagId(Long tagId);

    /**
     * 统计用户为好友分配的标签数量
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 标签数量
     */
    long countByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 查找用户使用指定标签的好友列表
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 好友ID列表
     */
    @Query("SELECT cta.friendId FROM ContactTagAssignment cta WHERE cta.userId = :userId AND cta.tagId = :tagId")
    List<Long> findFriendIdsByUserIdAndTagId(@Param("userId") Long userId, @Param("tagId") Long tagId);

    /**
     * 查找好友被分配的标签ID列表
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 标签ID列表
     */
    @Query("SELECT cta.tagId FROM ContactTagAssignment cta WHERE cta.userId = :userId AND cta.friendId = :friendId")
    List<Long> findTagIdsByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 删除特定的标签分配记录
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param tagId 标签ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM ContactTagAssignment cta WHERE cta.userId = :userId AND cta.friendId = :friendId AND cta.tagId = :tagId")
    int deleteByUserIdAndFriendIdAndTagId(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("tagId") Long tagId);

    /**
     * 批量查找用户为多个好友分配的标签
     * @param userId 用户ID
     * @param friendIds 好友ID列表
     * @return 标签分配列表
     */
    List<ContactTagAssignment> findByUserIdAndFriendIdIn(Long userId, List<Long> friendIds);
}