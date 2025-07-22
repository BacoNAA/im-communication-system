package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.request.ReportHandleRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.Report;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.repository.ReportRepository;
import com.im.imcommunicationsystem.admin.service.ReportService;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 举报服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;

    @Override
    public Page<ReportResponse> getReportListWithPagination(Pageable pageable, String status, String contentType) {
        log.info("获取举报列表，状态：{}，内容类型：{}", status, contentType);
        
        Page<Report> reportPage;
        
        // 根据过滤条件查询
        if (StringUtils.hasText(status) && StringUtils.hasText(contentType)) {
            // 状态和内容类型都有指定
            try {
                Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toLowerCase());
                reportPage = reportRepository.findByStatusAndReportedContentType(reportStatus, contentType, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("无效的举报状态：{}", status);
                reportPage = reportRepository.findByReportedContentType(contentType, pageable);
            }
        } else if (StringUtils.hasText(status)) {
            // 只有状态有指定
            try {
                Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toLowerCase());
                reportPage = reportRepository.findByStatus(reportStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("无效的举报状态：{}", status);
                reportPage = reportRepository.findAll(pageable);
            }
        } else if (StringUtils.hasText(contentType)) {
            // 只有内容类型有指定
            reportPage = reportRepository.findByReportedContentType(contentType, pageable);
        } else {
            // 没有任何过滤条件
            reportPage = reportRepository.findAll(pageable);
        }
        
        // 获取所有举报者和被举报者ID
        List<Long> userIds = new ArrayList<>();
        reportPage.getContent().forEach(report -> {
            userIds.add(report.getReporterId());
            if (report.getReportedUserId() != null) {
                userIds.add(report.getReportedUserId());
            }
        });
        
        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(user -> userMap.put(user.getId(), user));
        }
        
        // 转换为DTO
        List<ReportResponse> responseList = reportPage.getContent().stream()
                .map(report -> convertToReportResponse(report, userMap))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responseList, pageable, reportPage.getTotalElements());
    }

    @Override
    public ReportResponse getReportDetails(Long reportId) {
        log.info("获取举报详情，ID：{}", reportId);
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("举报不存在"));
        
        // 获取用户信息
        Map<Long, User> userMap = new HashMap<>();
        
        // 获取举报者信息
        Optional<User> reporterOpt = userRepository.findById(report.getReporterId());
        reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
        
        // 获取被举报者信息（如果有）
        if (report.getReportedUserId() != null) {
            Optional<User> reportedUserOpt = userRepository.findById(report.getReportedUserId());
            reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
        }
        
        return convertToReportResponse(report, userMap);
    }

    @Override
    @Transactional
    public ReportResponse handleReport(Long adminId, ReportHandleRequest request) {
        log.info("处理举报，管理员ID：{}，请求：{}", adminId, request);
        
        // 验证请求
        if (request == null || request.getReportId() == null) {
            throw new BusinessException("请求无效");
        }
        
        // 获取举报
        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new BusinessException("举报不存在"));
        
        // 检查举报是否已处理
        if (report.getStatus() != Report.ReportStatus.pending && report.getStatus() != Report.ReportStatus.processing) {
            throw new BusinessException("举报已处理，无法重复处理");
        }
        
        // 更新举报状态
        Report.ReportStatus newStatus;
        
        switch (request.getAction().toLowerCase()) {
            case "process":
                newStatus = Report.ReportStatus.processing;
                break;
            case "resolve":
                newStatus = Report.ReportStatus.resolved;
                break;
            case "reject":
                newStatus = Report.ReportStatus.rejected;
                break;
            default:
                throw new BusinessException("无效的操作类型");
        }
        
        report.setStatus(newStatus);
        report.setHandledAt(LocalDateTime.now());
        report.setHandledBy(adminId);
        
        // 保存举报
        Report updatedReport = reportRepository.save(report);
        
        // 记录操作日志
        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setAdminId(adminId);
        operationLog.setOperationTypeString("HANDLE_REPORT");
        operationLog.setTargetTypeString("REPORT");
        operationLog.setTargetId(report.getId());
        operationLog.setDescription(
                String.format("处理举报 #%d: %s，操作: %s，结果: %s", 
                        report.getId(), 
                        report.getReason(), 
                        request.getAction(),
                        request.getResult()));
        operationLog.setCreatedAt(LocalDateTime.now());
        
        adminOperationLogRepository.save(operationLog);
        
        // 返回更新后的举报信息
        Map<Long, User> userMap = new HashMap<>();
        
        Optional<User> reporterOpt = userRepository.findById(updatedReport.getReporterId());
        reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
        
        if (updatedReport.getReportedUserId() != null) {
            Optional<User> reportedUserOpt = userRepository.findById(updatedReport.getReportedUserId());
            reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
        }
        
        return convertToReportResponse(updatedReport, userMap);
    }

    @Override
    public Object getReportStatistics() {
        log.info("获取举报统计信息");
        
        // 统计各状态的举报数量
        Map<String, Object> statistics = new HashMap<>();
        Map<String, Long> statusCount = new HashMap<>();
        
        for (Report.ReportStatus status : Report.ReportStatus.values()) {
            long count = reportRepository.countByStatus(status);
            statusCount.put(status.name(), count);
        }
        
        statistics.put("statusCount", statusCount);
        
        // 统计各内容类型的举报数量
        List<Object[]> contentTypeCounts = reportRepository.countByContentType();
        Map<String, Long> contentTypeCount = new HashMap<>();
        
        for (Object[] result : contentTypeCounts) {
            String contentType = (String) result[0];
            Long count = (Long) result[1];
            if (contentType != null) {
                contentTypeCount.put(contentType, count);
            }
        }
        
        statistics.put("contentTypeCount", contentTypeCount);
        
        return statistics;
    }

    @Override
    @Transactional
    public ReportResponse createReport(Long reporterId, Long reportedUserId, String reportedContentType, 
                                    Long reportedContentId, String reason, String description) {
        log.info("创建新举报，举报者ID：{}，被举报用户ID：{}，内容类型：{}，内容ID：{}，原因：{}",
                reporterId, reportedUserId, reportedContentType, reportedContentId, reason);
        
        // 验证参数
        if (reporterId == null || !StringUtils.hasText(reportedContentType) || reportedContentId == null || !StringUtils.hasText(reason)) {
            throw new BusinessException("请求参数无效");
        }
        
        // 创建举报实体
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reportedContentType(reportedContentType)
                .reportedContentId(reportedContentId)
                .reason(reason)
                .description(description)
                .status(Report.ReportStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 保存举报
        Report savedReport = reportRepository.save(report);
        
        // 获取用户信息
        Map<Long, User> userMap = new HashMap<>();
        
        Optional<User> reporterOpt = userRepository.findById(reporterId);
        reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
        
        if (reportedUserId != null) {
            Optional<User> reportedUserOpt = userRepository.findById(reportedUserId);
            reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
        }
        
        return convertToReportResponse(savedReport, userMap);
    }
    
    /**
     * 将举报实体转换为DTO
     *
     * @param report 举报实体
     * @param userMap 用户映射表
     * @return 举报响应DTO
     */
    private ReportResponse convertToReportResponse(Report report, Map<Long, User> userMap) {
        User reporter = userMap.get(report.getReporterId());
        User reportedUser = report.getReportedUserId() != null ? userMap.get(report.getReportedUserId()) : null;
        
        return ReportResponse.builder()
                .reportId(report.getId())
                .reporterId(report.getReporterId())
                .reporterUsername(reporter != null ? reporter.getNickname() : "未知用户")
                .reportedUserId(report.getReportedUserId())
                .reportedUsername(reportedUser != null ? reportedUser.getNickname() : null)
                .reportedContentType(report.getReportedContentType())
                .reportedContentId(report.getReportedContentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .handledAt(report.getHandledAt())
                .handledBy(report.getHandledBy())
                .build();
    }
} 