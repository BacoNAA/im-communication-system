package com.im.imcommunicationsystem.moment.repository;

import com.im.imcommunicationsystem.moment.entity.MomentComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态评论数据仓库
 */
@Repository
public interface MomentCommentRepository extends JpaRepository<MomentComment, Long> {
    
    /**
     * 查找动态的评论列表（按创建时间升序）
     * 
     * @param momentId 动态ID
     * @param pageable 分页信息
     * @return 评论分页数据
     */
    Page<MomentComment> findByMomentIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long momentId, Pageable pageable);
    
    /**
     * 查找评论的回复列表（按创建时间升序）
     * 
     * @param parentCommentId 父评论ID
     * @param pageable 分页信息
     * @return 回复分页数据
     */
    Page<MomentComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId, Pageable pageable);
    
    /**
     * 统计动态评论数量
     * 
     * @param momentId 动态ID
     * @return 评论数量
     */
    long countByMomentId(Long momentId);
    
    /**
     * 根据ID和用户ID查找评论
     * 
     * @param id 评论ID
     * @param userId 用户ID
     * @return 评论对象
     */
    Optional<MomentComment> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 查找动态的所有评论及回复
     * 
     * @param momentId 动态ID
     * @return 评论列表
     */
    @Query("SELECT c FROM MomentComment c WHERE c.momentId = :momentId ORDER BY c.createdAt ASC")
    List<MomentComment> findAllCommentsAndRepliesByMomentId(@Param("momentId") Long momentId);
} 