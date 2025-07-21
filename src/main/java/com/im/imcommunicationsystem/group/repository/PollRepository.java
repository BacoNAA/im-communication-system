package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.Poll;
import com.im.imcommunicationsystem.group.enums.PollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 投票仓库接口
 */
@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    
    /**
     * 根据群组ID查询投票列表
     */
    List<Poll> findByGroupId(Long groupId);
    
    /**
     * 根据群组ID和状态查询投票列表
     */
    List<Poll> findByGroupIdAndStatus(Long groupId, PollStatus status);
    
    /**
     * 分页查询群组的投票
     */
    Page<Poll> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    
    /**
     * 分页查询群组特定状态的投票
     */
    Page<Poll> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, PollStatus status, Pageable pageable);
    
    /**
     * 查询特定创建者在特定群组创建的投票
     */
    List<Poll> findByGroupIdAndCreatorId(Long groupId, Long creatorId);
    
    /**
     * 根据ID查找投票，并包含创建者ID和群组ID限制
     */
    Optional<Poll> findByIdAndGroupIdAndCreatorId(Long id, Long groupId, Long creatorId);
} 