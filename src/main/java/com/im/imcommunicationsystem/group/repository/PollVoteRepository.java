package com.im.imcommunicationsystem.group.repository;

import com.im.imcommunicationsystem.group.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 投票记录仓库接口
 */
@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    
    /**
     * 根据投票ID和用户ID查询投票记录
     */
    List<PollVote> findByPollIdAndUserId(Long pollId, Long userId);
    
    /**
     * 检查用户是否已经投过票
     */
    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    
    /**
     * 根据选项ID统计投票数量
     */
    long countByOptionId(Long optionId);
    
    /**
     * 根据投票ID统计总票数
     */
    long countByPollId(Long pollId);
    
    /**
     * 根据投票ID和选项ID统计票数
     */
    long countByPollIdAndOptionId(Long pollId, Long optionId);
    
    /**
     * 根据投票ID获取每个选项的票数
     */
    @Query("SELECT v.optionId, COUNT(v) FROM PollVote v WHERE v.pollId = ?1 GROUP BY v.optionId")
    List<Object[]> countVotesByPollIdGroupByOptionId(Long pollId);
    
    /**
     * 根据多个投票ID一次性获取所有选项的票数
     */
    @Query("SELECT v.pollId, v.optionId, COUNT(v) FROM PollVote v WHERE v.pollId IN :pollIds GROUP BY v.pollId, v.optionId")
    List<Object[]> countVotesByPollIdsGroupByPollAndOption(@Param("pollIds") List<Long> pollIds);
    
    /**
     * 删除投票的所有投票记录
     */
    void deleteByPollId(Long pollId);
    
    /**
     * 查询用户对特定选项的投票
     */
    Optional<PollVote> findByPollIdAndOptionIdAndUserId(Long pollId, Long optionId, Long userId);
} 