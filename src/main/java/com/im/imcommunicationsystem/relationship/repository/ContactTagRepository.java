package com.im.imcommunicationsystem.relationship.repository;

import com.im.imcommunicationsystem.relationship.entity.ContactTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 好友标签数据访问接口
 * 提供好友标签的CRUD操作
 */
@Repository
public interface ContactTagRepository extends JpaRepository<ContactTag, Long> {

    /**
     * 查找用户的所有标签
     * @param userId 用户ID
     * @return 标签列表
     */
    List<ContactTag> findByUserId(Long userId);

    /**
     * 根据用户ID和标签名查找标签
     * @param userId 用户ID
     * @param name 标签名称
     * @return 标签
     */
    Optional<ContactTag> findByUserIdAndName(Long userId, String name);

    /**
     * 检查标签名是否存在
     * @param userId 用户ID
     * @param name 标签名称
     * @return 是否存在
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * 统计用户的标签数量
     * @param userId 用户ID
     * @return 标签数量
     */
    long countByUserId(Long userId);

    /**
     * 根据标签名模糊搜索
     * @param userId 用户ID
     * @param name 标签名称（模糊匹配）
     * @return 标签列表
     */
    @Query("SELECT ct FROM ContactTag ct WHERE ct.userId = :userId AND ct.name LIKE %:name%")
    List<ContactTag> findByUserIdAndNameContaining(@Param("userId") Long userId, @Param("name") String name);

    /**
     * 根据颜色查找标签
     * @param userId 用户ID
     * @param color 标签颜色
     * @return 标签列表
     */
    List<ContactTag> findByUserIdAndColor(Long userId, String color);

    /**
     * 查找用户最近创建的标签
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最近创建的标签列表
     */
    @Query("SELECT ct FROM ContactTag ct WHERE ct.userId = :userId " +
           "ORDER BY ct.createdAt DESC LIMIT :limit")
    List<ContactTag> findRecentTags(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 删除用户的所有标签
     * @param userId 用户ID
     * @return 删除的记录数
     */
    long deleteByUserId(Long userId);
}