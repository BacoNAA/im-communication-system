package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.ContentModerationRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.Report;
import com.im.imcommunicationsystem.admin.exception.ContentModerationException;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.repository.ReportRepository;
import com.im.imcommunicationsystem.admin.service.ContentModerationService;
import com.im.imcommunicationsystem.admin.utils.AdminPermissionUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容审核服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentModerationServiceImpl implements ContentModerationService {

    private final ReportRepository reportRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;
    private final AdminPermissionUtils adminPermissionUtils;

    @Override
    public Page<ReportResponse> getReportListWithPagination(Pageable pageable, String status, String contentType) {
        Page<Report> reports;

        if (status != null && contentType != null) {
            Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status);
            reports = reportRepository.findByStatusAndReportedContentType(reportStatus, contentType, pageable);
        } else if (status != null) {
            Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status);
            reports = reportRepository.findByStatus(reportStatus, pageable);
        } else if (contentType != null) {
            reports = reportRepository.findByReportedContentType(contentType, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }

        return reports.map(this::convertToReportResponse);
    }

    @Override
    public ReportResponse getReportDetails(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + reportId + " 的举报"));

        return convertToReportResponse(report);
    }

    @Override
    @Transactional
    public ReportResponse handleReport(Long adminId, ContentModerationRequest request) {
        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为: " + request.getReportId() + " 的举报"));

        if (report.getStatus() != Report.ReportStatus.pending) {
            throw new ContentModerationException("该举报已经被处理过");
        }

        // 更新举报状态
        Report.ReportStatus newStatus;
        switch (request.getAction()) {
            case "resolve":
                newStatus = Report.ReportStatus.resolved;
                break;
            case "reject":
                newStatus = Report.ReportStatus.rejected;
                break;
            default:
                newStatus = Report.ReportStatus.processing;
                break;
        }
        
        report.setStatus(newStatus);
        report.setHandledAt(LocalDateTime.now());
        report.setHandledBy(adminId);

        // 如果举报被接受，处理被举报的内容
        if ("resolve".equals(request.getAction())) {
            handleReportedContent(report);
        }

        // 保存管理员操作日志
        AdminOperationLog log = AdminOperationLog.builder()
                .adminId(adminId)
                .operationType("HANDLE_REPORT")
                .targetType(report.getReportedContentType())
                .targetId(report.getReportedContentId())
                .description("处理举报: " + request.getAction() + ", 备注: " + request.getNote())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogRepository.save(log);

        // 保存更新后的举报
        Report savedReport = reportRepository.save(report);

        return convertToReportResponse(savedReport);
    }

    @Override
    public Object getContentModerationStatistics() {
        // 获取不同举报类型和状态的数量
        long totalReports = reportRepository.count();
        long pendingReports = reportRepository.countByStatus(Report.ReportStatus.pending);
        long processingReports = reportRepository.countByStatus(Report.ReportStatus.processing);
        long resolvedReports = reportRepository.countByStatus(Report.ReportStatus.resolved);
        long rejectedReports = reportRepository.countByStatus(Report.ReportStatus.rejected);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalReports", totalReports);
        statistics.put("pendingReports", pendingReports);
        statistics.put("processingReports", processingReports);
        statistics.put("resolvedReports", resolvedReports);
        statistics.put("rejectedReports", rejectedReports);

        // 获取按内容类型统计的举报数量
        Map<String, Long> reportsByContentType = new HashMap<>();
        List<Object[]> contentTypeCounts = reportRepository.countByContentType();
        for (Object[] count : contentTypeCounts) {
            reportsByContentType.put((String) count[0], (Long) count[1]);
        }
        statistics.put("reportsByContentType", reportsByContentType);

        return statistics;
    }

    /**
     * 将举报实体转换为举报响应DTO
     *
     * @param report 举报实体
     * @return 举报响应DTO
     */
    private ReportResponse convertToReportResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getId())
                .reporterId(report.getReporterId())
                .reporterUsername("User_" + report.getReporterId()) // 简化处理
                .reportedUserId(report.getReportedUserId())
                .reportedUsername(report.getReportedUserId() != null ? "User_" + report.getReportedUserId() : null) // 简化处理
                .reportedContentType(report.getReportedContentType())
                .reportedContentId(report.getReportedContentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus().toString())
                .createdAt(report.getCreatedAt())
                .handledAt(report.getHandledAt())
                .handledBy(report.getHandledBy())
                .build();
    }

    /**
     * 处理被举报的内容
     *
     * @param report 举报实体
     */
    private void handleReportedContent(Report report) {
        String contentType = report.getReportedContentType();
        Long contentId = report.getReportedContentId();

        log.info("处理被举报内容: 类型={}, ID={}", contentType, contentId);
        // 处理被举报内容的逻辑将在这里实现
        // 这通常涉及调用其他服务或仓库
        // 来标记内容为隐藏、标记用户等
    }
} 