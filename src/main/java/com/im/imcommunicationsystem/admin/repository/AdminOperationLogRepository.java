package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员操作日志仓库接口
 */
@Repository
public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    /**
     * 根据管理员ID和日期范围查找操作日志
     *
     * @param adminId 管理员ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByAdminIdAndCreatedAtBetween(
            Long adminId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 根据操作类型查找操作日志
     *
     * @param operationType 操作类型
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByOperationType(String operationType, Pageable pageable);

    /**
     * 根据目标类型和目标ID查找操作日志
     *
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);

    /**
     * 获取最近的管理员操作日志
     *
     * @param adminId 管理员ID
     * @param limit 限制数量
     * @return 操作日志列表
     */
    @Query("SELECT a FROM AdminOperationLog a WHERE a.adminId = :adminId ORDER BY a.createdAt DESC")
    List<AdminOperationLog> findRecentLogsByAdminId(@Param("adminId") Long adminId, Pageable pageable);

    /**
     * 统计指定时间段内各类型操作的数量
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 操作类型和数量的列表
     */
    @Query("SELECT a.operationType, COUNT(a) FROM AdminOperationLog a " +
           "WHERE a.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY a.operationType")
    List<Object[]> countOperationTypesBetween(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 