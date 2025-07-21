package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
} 