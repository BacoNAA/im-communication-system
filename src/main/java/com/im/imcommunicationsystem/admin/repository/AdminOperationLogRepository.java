package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员操作日志仓库接口
 */
@Repository
public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    /**
     * 根据管理员ID查询操作日志
     * @param adminId 管理员ID
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByAdminId(Long adminId, Pageable pageable);

    /**
     * 根据操作类型查询操作日志
     * @param operationTypeString 操作类型
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByOperationTypeString(String operationTypeString, Pageable pageable);

    /**
     * 根据目标类型和目标ID查询操作日志
     * @param targetTypeString 目标类型
     * @param targetId 目标ID
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByTargetTypeStringAndTargetId(String targetTypeString, Long targetId, Pageable pageable);

    /**
     * 根据时间范围查询操作日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页信息
     * @return 操作日志分页结果
     */
    Page<AdminOperationLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据管理员ID和操作类型查询操作日志
     * @param adminId 管理员ID
     * @param operationTypeString 操作类型
     * @return 操作日志列表
     */
    List<AdminOperationLog> findByAdminIdAndOperationTypeString(Long adminId, String operationTypeString);
} 