package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.GroupJoinRequest;
import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 群组加入请求数据访问接口
 */
@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    
    /**
     * 查询群组的加入请求
     */
    List<GroupJoinRequest> findByGroupIdAndStatus(Long groupId, GroupJoinRequestStatus status);
    
    /**
     * 分页查询群组的加入请求
     */
    Page<GroupJoinRequest> findByGroupIdAndStatusOrderByCreatedAtDesc(
            Long groupId, GroupJoinRequestStatus status, Pageable pageable);
    
    /**
     * 分页查询群组的所有加入请求（不过滤状态）
     */
    Page<GroupJoinRequest> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    
    /**
     * 查询用户的加入请求
     */
    List<GroupJoinRequest> findByUserIdAndStatus(Long userId, GroupJoinRequestStatus status);
    
    /**
     * 分页查询用户的加入请求
     */
    Page<GroupJoinRequest> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, GroupJoinRequestStatus status, Pageable pageable);
    
    /**
     * 查询用户对特定群组的加入请求
     */
    Optional<GroupJoinRequest> findByGroupIdAndUserIdAndStatus(
            Long groupId, Long userId, GroupJoinRequestStatus status);
    
    /**
     * 检查是否存在用户对群组的待处理请求
     */
    boolean existsByGroupIdAndUserIdAndStatus(
            Long groupId, Long userId, GroupJoinRequestStatus status);
    
    /**
     * 统计群组的待处理请求数量
     */
    long countByGroupIdAndStatus(Long groupId, GroupJoinRequestStatus status);
} 