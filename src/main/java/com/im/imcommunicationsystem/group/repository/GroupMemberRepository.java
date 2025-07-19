package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.entity.GroupMemberId;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 群成员数据访问接口
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    
    /**
     * 查询群组所有成员
     */
    List<GroupMember> findByIdGroupId(Long groupId);
    
    /**
     * 分页查询群组成员
     */
    Page<GroupMember> findByIdGroupId(Long groupId, Pageable pageable);
    
    /**
     * 查询特定用户在群组中的信息
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.id.groupId = :groupId AND gm.id.userId = :userId")
    Optional<GroupMember> findByIdGroupIdAndIdUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 统计群组成员数量
     */
    long countByIdGroupId(Long groupId);
    
    /**
     * 查询用户管理的群组
     */
    List<GroupMember> findByIdUserIdAndRoleIn(Long userId, List<GroupMemberRole> roles);
    
    /**
     * 查询群组的管理员
     */
    List<GroupMember> findByIdGroupIdAndRoleIn(Long groupId, List<GroupMemberRole> roles);
    
    /**
     * 查询群主
     */
    Optional<GroupMember> findByIdGroupIdAndRole(Long groupId, GroupMemberRole role);
    
    /**
     * 删除群成员
     */
    void deleteByIdGroupIdAndIdUserId(Long groupId, Long userId);
    
    /**
     * 批量删除群成员
     */
    void deleteByIdGroupIdAndIdUserIdIn(Long groupId, List<Long> userIds);
    
    /**
     * 查询用户参与的所有群组成员记录
     */
    List<GroupMember> findByIdUserId(Long userId);
} 