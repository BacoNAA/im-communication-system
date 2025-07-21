package com.im.imcommunicationsystem.admin.service;

import com.im.imcommunicationsystem.admin.dto.request.ContentModerationRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 内容审核服务接口
 */
public interface ContentModerationService {

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
     * 处理举报
     *
     * @param adminId 管理员ID
     * @param request 内容审核请求
     * @return 处理后的举报响应对象
     */
    ReportResponse handleReport(Long adminId, ContentModerationRequest request);

    /**
     * 获取内容审核统计信息
     *
     * @return 内容审核统计数据
     */
    Object getContentModerationStatistics();
} 