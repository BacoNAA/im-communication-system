package com.im.imcommunicationsystem.admin.controller;

import com.im.imcommunicationsystem.admin.dto.response.SystemOverviewResponse;
import com.im.imcommunicationsystem.admin.dto.response.StatisticsResponse;
import com.im.imcommunicationsystem.admin.service.AdminDashboardService;
import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import com.im.imcommunicationsystem.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员仪表盘控制器
 * 提供系统概览数据接口
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final SecurityUtils securityUtils;

    /**
     * 获取系统概览数据
     * @param period 时间周期 (today, week, month, year)
     * @return 系统概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<ResponseUtils.ApiResponse<SystemOverviewResponse>> getSystemOverview(
            @RequestParam(required = false, defaultValue = "today") String period) {
        
        log.info("获取系统概览数据请求, period={}", period);
        
        // 获取当前管理员ID
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId == null) {
            log.error("获取系统概览失败：未获取到管理员ID");
            return ResponseEntity.status(401).body(ResponseUtils.error(401, "未认证，请先登录"));
        }
        
        try {
            SystemOverviewResponse overview = adminDashboardService.getSystemOverview(period);
            return ResponseEntity.ok(ResponseUtils.success(overview));
        } catch (Exception e) {
            log.error("获取系统概览数据失败", e);
            return ResponseEntity.status(500).body(ResponseUtils.error(500, "获取系统概览数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取特定统计数据的趋势
     * @param type 统计类型 (users, messages, groups, moments)
     * @param period 时间周期 (today, week, month, year)
     * @return 统计数据趋势
     */
    @GetMapping("/statistics/{type}")
    public ResponseEntity<ResponseUtils.ApiResponse<StatisticsResponse>> getStatistics(
            @PathVariable String type,
            @RequestParam(required = false, defaultValue = "week") String period) {
        
        log.info("获取统计数据请求, type={}, period={}", type, period);
        
        // 获取当前管理员ID
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId == null) {
            log.error("获取统计数据失败：未获取到管理员ID");
            return ResponseEntity.status(401).body(ResponseUtils.error(401, "未认证，请先登录"));
        }
        
        try {
            StatisticsResponse statistics = adminDashboardService.getStatistics(type, period);
            return ResponseEntity.ok(ResponseUtils.success(statistics));
        } catch (IllegalArgumentException e) {
            log.error("获取统计数据请求参数错误", e);
            return ResponseEntity.badRequest().body(ResponseUtils.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ResponseEntity.status(500).body(ResponseUtils.error(500, "获取统计数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户活跃度分布
     * @param period 时间周期 (today, week, month, year)
     * @return 用户活跃度分布数据
     */
    @GetMapping("/user-activity")
    public ResponseEntity<ResponseUtils.ApiResponse<Map<String, Object>>> getUserActivityDistribution(
            @RequestParam(required = false, defaultValue = "week") String period) {
        
        log.info("获取用户活跃度分布请求, period={}", period);
        
        // 获取当前管理员ID
        Long adminId = securityUtils.getCurrentUserId();
        if (adminId == null) {
            log.error("获取用户活跃度分布失败：未获取到管理员ID");
            return ResponseEntity.status(401).body(ResponseUtils.error(401, "未认证，请先登录"));
        }
        
        try {
            Map<String, Object> activityData = adminDashboardService.getUserActivityDistribution(period);
            return ResponseEntity.ok(ResponseUtils.success(activityData));
        } catch (Exception e) {
            log.error("获取用户活跃度分布失败", e);
            return ResponseEntity.status(500).body(ResponseUtils.error(500, "获取用户活跃度分布失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取内容类型分布数据
     * @param period 时间周期 (today, week, month, year)
     * @return 内容类型分布数据
     */
    @GetMapping("/content-distribution")
    public ResponseEntity<ResponseUtils.ApiResponse<Map<String, Object>>> getContentTypeDistribution(
            @RequestParam(required = false, defaultValue = "week") String period) {
        
        // 验证管理员权限
        Long userId = securityUtils.getCurrentUserId();
        
        try {
            Map<String, Object> data = adminDashboardService.getContentTypeDistribution(period);
            return ResponseEntity.ok(ResponseUtils.success(data));
        } catch (Exception e) {
            log.error("获取内容类型分布数据失败: ", e);
            return ResponseEntity.status(500).body(ResponseUtils.error(500, "获取内容类型分布数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取消息趋势数据
     * @param period 时间周期 (today, week, month, year)
     * @return 消息趋势数据
     */
    @GetMapping("/message-trend")
    public ResponseEntity<ResponseUtils.ApiResponse<StatisticsResponse>> getMessageTrend(
            @RequestParam(required = false, defaultValue = "week") String period) {
        
        // 验证管理员权限
        Long userId = securityUtils.getCurrentUserId();
        
        try {
            StatisticsResponse data = adminDashboardService.getStatistics("messages", period);
            return ResponseEntity.ok(ResponseUtils.success(data));
        } catch (Exception e) {
            log.error("获取消息趋势数据失败: ", e);
            return ResponseEntity.status(500).body(ResponseUtils.error(500, "获取消息趋势数据失败: " + e.getMessage()));
        }
    }
} 