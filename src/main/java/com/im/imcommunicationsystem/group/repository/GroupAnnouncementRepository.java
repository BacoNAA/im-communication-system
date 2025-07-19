package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.GroupAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 群公告数据访问接口
 */
@Repository
public interface GroupAnnouncementRepository extends JpaRepository<GroupAnnouncement, Long> {
    
    /**
     * 按时间倒序获取群组公告列表
     */
    List<GroupAnnouncement> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    
    /**
     * 分页获取群组公告
     */
    Page<GroupAnnouncement> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    
    /**
     * 获取置顶公告
     */
    Optional<GroupAnnouncement> findByGroupIdAndIsPinnedTrue(Long groupId);
    
    /**
     * 统计群组公告数量
     */
    long countByGroupId(Long groupId);
    
    /**
     * 删除群组的所有公告
     */
    void deleteByGroupId(Long groupId);
} 