package com.im.imcommunicationsystem.moment.repository;

import com.im.imcommunicationsystem.moment.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态数据仓库
 */
@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    
    /**
     * 按时间倒序查找用户动态
     * 
     * @param userId 用户ID
     * @param pageable 分页信息
     * @return 动态分页数据
     */
    Page<Moment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 查找多个用户的动态
     * 
     * @param userIds 用户ID列表
     * @param pageable 分页信息
     * @return 动态分页数据
     */
    Page<Moment> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
    
    /**
     * 根据ID和用户ID查找动态
     * 
     * @param id 动态ID
     * @param userId 用户ID
     * @return 动态对象
     */
    Optional<Moment> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 统计用户动态数量
     * 
     * @param userId 用户ID
     * @return 动态数量
     */
    long countByUserId(Long userId);
    
    /**
     * 查找好友的动态，包含所有可见性类型的动态
     * 
     * @param userIds 好友ID列表
     * @param pageable 分页信息
     * @return 动态分页数据
     */
    @Query("SELECT m FROM Moment m WHERE m.userId IN :userIds ORDER BY m.createdAt DESC")
    Page<Moment> findFriendMoments(@Param("userIds") List<Long> userIds, Pageable pageable);
} 