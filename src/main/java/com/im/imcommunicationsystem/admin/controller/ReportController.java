package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.ReportHandleRequest;
import com.im.imcommunicationsystem.admin.dto.request.CreateReportRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import com.im.imcommunicationsystem.admin.service.ReportService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    /**
     * 用户提交举报
     *
     * @param request 举报请求
     * @return 举报响应
     */
    @PostMapping("/api/reports")
    public ResponseEntity<ResponseUtils.ApiResponse<ReportResponse>> submitReport(
            @Valid @RequestBody CreateReportRequest request) {
        
        // 获取当前用户ID
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("未授权的举报请求");
            return ResponseEntity.status(401).body(ResponseUtils.error(401, "未授权，请先登录"));
        }
        
        log.info("用户ID:{}提交举报，内容类型:{}，内容ID:{}", userId, request.getReportedContentType(), request.getReportedContentId());
        
        ReportResponse report = reportService.createReport(
                userId,
                request.getReportedUserId(),
                request.getReportedContentType(),
                request.getReportedContentId(),
                request.getReason(),
                request.getDescription()
        );
        
        return ResponseEntity.ok(ResponseUtils.success("举报提交成功，我们将尽快处理", report));
    }
    
    /**
     * 管理员获取举报列表
     *
     * @param pageable 分页信息
     * @param status 状态过滤
     * @param contentType 内容类型过滤
     * @return 举报列表
     */
    @GetMapping("/api/admin/reports")
    public ResponseEntity<ResponseUtils.ApiResponse<Page<ReportResponse>>> getReportList(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contentType) {
        
        log.info("获取举报列表，状态: {}, 内容类型: {}", status, contentType);
        Page<ReportResponse> reports = reportService.getReportListWithPagination(pageable, status, contentType);
        
        return ResponseEntity.ok(ResponseUtils.success("举报列表获取成功", reports));
    }

    /**
     * 获取举报详情
     *
     * @param reportId 举报ID
     * @return 举报详情
     */
    @GetMapping("/api/admin/reports/{reportId}")
    public ResponseEntity<ResponseUtils.ApiResponse<ReportResponse>> getReportDetails(@PathVariable Long reportId) {
        log.info("获取举报详情，ID: {}", reportId);
        ReportResponse report = reportService.getReportDetails(reportId);
        
        return ResponseEntity.ok(ResponseUtils.success("举报详情获取成功", report));
    }

    /**
     * 处理举报
     *
     * @param reportId 举报ID
     * @param request 举报处理请求
     * @param userDetails 已认证的管理员用户
     * @return 处理结果
     */
    @PostMapping("/api/admin/reports/{reportId}/handle")
    public ResponseEntity<ResponseUtils.ApiResponse<ReportResponse>> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportHandleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("处理举报，举报ID: {}，操作: {}", reportId, request.getAction());
        
        // 设置举报ID
        request.setReportId(reportId);
        
        // 从已认证用户中提取管理员ID
        Long adminId = Long.valueOf(userDetails.getUsername());
        
        ReportResponse updatedReport = reportService.handleReport(adminId, request);
        
        return ResponseEntity.ok(ResponseUtils.success("举报处理成功", updatedReport));
    }

    /**
     * 获取举报统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/api/admin/reports/statistics")
    public ResponseEntity<ResponseUtils.ApiResponse<Object>> getReportStatistics() {
        log.info("获取举报统计信息");
        Object statistics = reportService.getReportStatistics();
        
        return ResponseEntity.ok(ResponseUtils.success("举报统计信息获取成功", statistics));
    }
} 