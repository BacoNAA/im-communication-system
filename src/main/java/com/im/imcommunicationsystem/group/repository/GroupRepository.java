package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 群组数据访问接口
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long>, JpaSpecificationExecutor<Group> {
    
    /**
     * 根据群主ID查询群组
     */
    List<Group> findByOwnerId(Long ownerId);
    
    /**
     * 根据群主ID查询群组（分页）
     */
    Page<Group> findByOwnerId(Long ownerId, Pageable pageable);
    
    /**
     * 根据群组名称模糊查询
     */
    Page<Group> findByNameContaining(String name, Pageable pageable);
    
    /**
     * 统计用户创建的群组数量
     */
    long countByOwnerId(Long ownerId);
    
    /**
     * 查询用户参与的群组
     */
    @Query("SELECT g FROM Group g JOIN GroupMember m ON g.id = m.id.groupId WHERE m.id.userId = :userId")
    Page<Group> findGroupsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 查询用户是否在群组中
     */
    @Query("SELECT COUNT(m) > 0 FROM GroupMember m WHERE m.id.groupId = :groupId AND m.id.userId = :userId")
    boolean existsByIdGroupIdAndIdUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
} 