package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.request.ContentModerationRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import com.im.imcommunicationsystem.admin.service.ContentModerationService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
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

import java.util.Map;

/**
 * 内容审核控制器
 */
@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
@Slf4j
public class ContentModerationController {

    private final ContentModerationService contentModerationService;

    /**
     * 获取举报列表（分页）
     *
     * @param pageable 分页信息
     * @param status 状态过滤
     * @param contentType 内容类型过滤
     * @return 包含举报列表的响应
     */
    @GetMapping("/reports")
    public ResponseEntity<ResponseUtils.ApiResponse<Page<ReportResponse>>> getReportList(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contentType) {
        
        log.info("获取举报列表，状态: {}, 内容类型: {}", status, contentType);
        Page<ReportResponse> reports = contentModerationService.getReportListWithPagination(pageable, status, contentType);
        
        return ResponseEntity.ok(ResponseUtils.success("举报列表获取成功", reports));
    }

    /**
     * 获取举报详情
     *
     * @param reportId 举报ID
     * @return 包含举报详情的响应
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ResponseUtils.ApiResponse<ReportResponse>> getReportDetails(@PathVariable Long reportId) {
        log.info("获取举报详情，举报ID: {}", reportId);
        ReportResponse report = contentModerationService.getReportDetails(reportId);
        
        return ResponseEntity.ok(ResponseUtils.success("举报详情获取成功", report));
    }

    /**
     * 处理举报
     *
     * @param reportId 举报ID
     * @param request 内容审核请求
     * @param userDetails 已认证的管理员用户
     * @return 包含更新后举报的响应
     */
    @PostMapping("/reports/{reportId}/handle")
    public ResponseEntity<ResponseUtils.ApiResponse<ReportResponse>> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ContentModerationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("处理举报，举报ID: {}，操作: {}", reportId, request.getAction());
        
        // 从路径变量设置举报ID
        request.setReportId(reportId);
        
        // 从已认证用户中提取管理员ID
        Long adminId = Long.valueOf(userDetails.getUsername());
        
        ReportResponse updatedReport = contentModerationService.handleReport(adminId, request);
        
        return ResponseEntity.ok(ResponseUtils.success("举报处理成功", updatedReport));
    }

    /**
     * 获取内容审核统计信息
     *
     * @return 包含审核统计的响应
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseUtils.ApiResponse<Object>> getModerationStatistics() {
        log.info("获取内容审核统计信息");
        Object statistics = contentModerationService.getContentModerationStatistics();
        
        return ResponseEntity.ok(ResponseUtils.success("内容审核统计信息获取成功", statistics));
    }
} 