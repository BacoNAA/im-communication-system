package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 投票选项仓库接口
 */
@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    
    /**
     * 根据投票ID查询选项列表
     */
    List<PollOption> findByPollIdOrderByDisplayOrder(Long pollId);
    
    /**
     * 统计投票的选项数量
     */
    long countByPollId(Long pollId);
    
    /**
     * 删除投票的所有选项
     */
    void deleteByPollId(Long pollId);
    
    /**
     * 使用原生SQL删除投票的所有选项(避免乐观锁冲突)
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM poll_options WHERE poll_id = :pollId", nativeQuery = true)
    int deleteByPollIdNative(@Param("pollId") Long pollId);
    
    /**
     * 使用原生SQL删除指定群组的所有投票选项
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM poll_options WHERE poll_id IN (SELECT id FROM polls WHERE group_id = :groupId)", nativeQuery = true)
    int deleteAllByGroupId(@Param("groupId") Long groupId);
} 