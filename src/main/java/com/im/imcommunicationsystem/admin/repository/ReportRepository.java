package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报信息仓库接口
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    /**
     * 根据状态和举报内容类型查找举报
     * 
     * @param status 举报状态
     * @param reportedContentType 举报内容类型
     * @param pageable 分页信息
     * @return 举报分页结果
     */
    Page<Report> findByStatusAndReportedContentType(Report.ReportStatus status, String reportedContentType, Pageable pageable);
    
    /**
     * 根据状态查找举报
     * 
     * @param status 举报状态
     * @param pageable 分页信息
     * @return 举报分页结果
     */
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    
    /**
     * 根据举报者ID查找举报
     * 
     * @param reporterId 举报者ID
     * @param pageable 分页信息
     * @return 举报分页结果
     */
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
    
    /**
     * 根据被举报用户ID查找举报
     * 
     * @param reportedUserId 被举报用户ID
     * @param pageable 分页信息
     * @return 举报分页结果
     */
    Page<Report> findByReportedUserId(Long reportedUserId, Pageable pageable);
    
    /**
     * 根据举报内容类型查找举报
     * 
     * @param reportedContentType 举报内容类型
     * @param pageable 分页信息
     * @return 举报分页结果
     */
    Page<Report> findByReportedContentType(String reportedContentType, Pageable pageable);
    
    /**
     * 统计指定状态的举报数量
     * 
     * @param status 举报状态
     * @return 举报数量
     */
    long countByStatus(Report.ReportStatus status);
    
    /**
     * 统计各内容类型的举报数量
     * 
     * @return 内容类型和数量的列表
     */
    @Query("SELECT r.reportedContentType, COUNT(r) FROM Report r GROUP BY r.reportedContentType")
    List<Object[]> countByContentType();
} 