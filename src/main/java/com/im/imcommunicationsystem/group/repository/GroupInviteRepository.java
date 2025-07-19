package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.GroupInvite;
import com.im.imcommunicationsystem.group.enums.GroupInviteStatus;
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
 * 群组邀请数据访问接口
 */
@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    
    /**
     * 查询群组待处理邀请
     */
    List<GroupInvite> findByGroupIdAndStatus(Long groupId, GroupInviteStatus status);
    
    /**
     * 查询用户收到的邀请
     */
    List<GroupInvite> findByInviteeIdAndStatus(Long inviteeId, GroupInviteStatus status);
    
    /**
     * 分页查询用户收到的邀请
     */
    Page<GroupInvite> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, GroupInviteStatus status, Pageable pageable);
    
    /**
     * 查询特定邀请
     */
    Optional<GroupInvite> findByIdAndGroupId(Long id, Long groupId);
    
    /**
     * 查询用户在特定群组的邀请
     */
    Optional<GroupInvite> findByGroupIdAndInviteeIdAndStatus(Long groupId, Long inviteeId, GroupInviteStatus status);
    
    /**
     * 更新过期邀请状态
     */
    @Modifying
    @Query("UPDATE GroupInvite i SET i.status = :status WHERE i.status = :currentStatus AND i.expiresAt < :now")
    int updateExpiredInvites(@Param("status") GroupInviteStatus status, @Param("currentStatus") GroupInviteStatus currentStatus, @Param("now") LocalDateTime now);
    
    /**
     * 删除群组的所有邀请
     */
    void deleteByGroupId(Long groupId);
} 