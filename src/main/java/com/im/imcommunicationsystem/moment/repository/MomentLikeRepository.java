package com.im.imcommunicationsystem.moment.repository;

import com.im.imcommunicationsystem.moment.entity.MomentLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态点赞数据仓库
 */
@Repository
public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {
    
    /**
     * 查找动态的点赞列表
     * 
     * @param momentId 动态ID
     * @param pageable 分页信息
     * @return 点赞分页数据
     */
    Page<MomentLike> findByMomentIdOrderByCreatedAtDesc(Long momentId, Pageable pageable);
    
    /**
     * 查找特定用户对动态的点赞
     * 
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 点赞对象
     */
    Optional<MomentLike> findByMomentIdAndUserId(Long momentId, Long userId);
    
    /**
     * 统计动态点赞数量
     * 
     * @param momentId 动态ID
     * @return 点赞数量
     */
    long countByMomentId(Long momentId);
    
    /**
     * 删除特定用户的点赞
     * 
     * @param momentId 动态ID
     * @param userId 用户ID
     * @return 删除的记录数量
     */
    long deleteByMomentIdAndUserId(Long momentId, Long userId);
    
    /**
     * 查找用户点赞的动态ID列表
     * 
     * @param userId 用户ID
     * @return 动态ID列表
     */
    List<Long> findMomentIdsByUserId(Long userId);
} 