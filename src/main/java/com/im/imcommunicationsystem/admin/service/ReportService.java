package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.ReportHandleRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 举报服务接口
 */
public interface ReportService {

    /**
     * 获取举报列表（分页）
     *
     * @param pageable 分页信息
     * @param status 状态过滤（可选）
     * @param contentType 内容类型过滤（可选）
     * @return 举报响应对象分页结果
     */
    Page<ReportResponse> getReportListWithPagination(Pageable pageable, String status, String contentType);

    /**
     * 获取举报详情
     *
     * @param reportId 举报ID
     * @return 举报响应对象
     */
    ReportResponse getReportDetails(Long reportId);

    /**
     * 获取被举报内容的详情
     *
     * @param contentType 内容类型（USER, MESSAGE, GROUP, GROUP_MEMBER, MOMENT等）
     * @param contentId 内容ID
     * @return 内容详情，包含基本信息和特定类型的信息
     */
    Map<String, Object> getReportedContentDetails(String contentType, Long contentId);

    /**
     * 处理举报
     *
     * @param adminId 管理员ID
     * @param request 举报处理请求
     * @return 处理后的举报响应对象
     */
    ReportResponse handleReport(Long adminId, ReportHandleRequest request);

    /**
     * 获取举报统计信息
     *
     * @return 举报统计数据
     */
    Object getReportStatistics();
    
    /**
     * 创建新举报
     *
     * @param reporterId 举报者ID
     * @param reportedUserId 被举报用户ID（可选）
     * @param reportedContentType 被举报内容类型
     * @param reportedContentId 被举报内容ID
     * @param reason 举报原因
     * @param description 举报描述
     * @return 创建的举报响应对象
     */
    ReportResponse createReport(Long reporterId, Long reportedUserId, String reportedContentType, 
                              Long reportedContentId, String reason, String description);
} 