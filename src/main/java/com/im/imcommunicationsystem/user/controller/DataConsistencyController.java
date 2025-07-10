package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.user.service.DataConsistencyService;
// Swagger注解已移除，因为项目中没有相关依赖
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据一致性管理控制器
 * 提供数据库与MinIO之间数据一致性检查和修复功能
 */
@RestController
@RequestMapping("/api/admin/data-consistency")
@RequiredArgsConstructor
@Slf4j
// 数据一致性管理控制器
public class DataConsistencyController {

    private final DataConsistencyService dataConsistencyService;

    /**
     * 检查数据一致性
     */
    @GetMapping("/check")
    // 检查数据一致性
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> checkDataConsistency() {
        try {
            log.info("管理员执行数据一致性检查");
            
            DataConsistencyService.ConsistencyCheckResult result = dataConsistencyService.checkDataConsistency();
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasInconsistencies", result.hasInconsistencies());
            response.put("totalInconsistencies", result.getTotalInconsistencies());
            response.put("orphanedDbRecords", result.getOrphanedDbRecords());
            response.put("orphanedMinioFiles", result.getOrphanedMinioFiles());
            response.put("inconsistentRecords", result.getInconsistentRecords());
            
            if (result.hasInconsistencies()) {
                log.warn("发现数据不一致问题，总计: {} 个", result.getTotalInconsistencies());
                return ApiResponse.success("发现数据不一致问题，请查看详细信息", response);
            } else {
                log.info("数据一致性检查通过，未发现问题");
                return ApiResponse.success("数据一致性检查通过", response);
            }
            
        } catch (Exception e) {
            log.error("数据一致性检查失败", e);
            return ApiResponse.error(500, "数据一致性检查失败: " + e.getMessage());
        }
    }

    /**
     * 修复数据一致性
     */
    @PostMapping("/repair")
    // 修复数据一致性
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> repairDataConsistency(
            // 是否修复孤立的数据库记录（将其标记为已删除）
            @RequestParam(defaultValue = "true") boolean repairOrphanedDbRecords) {
        try {
            log.info("管理员执行数据一致性修复，修复孤立数据库记录: {}", repairOrphanedDbRecords);
            
            DataConsistencyService.RepairResult result = dataConsistencyService.repairDataConsistency(repairOrphanedDbRecords);
            
            Map<String, Object> response = new HashMap<>();
            response.put("repairedDbRecords", result.getRepairedDbRecords());
            response.put("failedRepairs", result.getFailedRepairs());
            response.put("totalRepaired", result.getTotalRepaired());
            
            if (result.getTotalRepaired() > 0) {
                log.info("数据一致性修复完成，修复了 {} 个记录", result.getTotalRepaired());
                return ApiResponse.success(String.format("修复完成，共修复 %d 个记录", result.getTotalRepaired()), response);
            } else {
                log.info("数据一致性修复完成，无需修复的记录");
                return ApiResponse.success("无需修复的数据不一致问题", response);
            }
            
        } catch (Exception e) {
            log.error("数据一致性修复失败", e);
            return ApiResponse.error(500, "数据一致性修复失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据一致性状态概览
     */
    @GetMapping("/status")
    // 获取数据一致性状态
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getConsistencyStatus() {
        try {
            log.info("管理员查询数据一致性状态");
            
            DataConsistencyService.ConsistencyCheckResult result = dataConsistencyService.checkDataConsistency();
            
            Map<String, Object> status = new HashMap<>();
            status.put("isConsistent", !result.hasInconsistencies());
            status.put("lastCheckTime", java.time.LocalDateTime.now());
            status.put("totalInconsistencies", result.getTotalInconsistencies());
            status.put("orphanedDbRecordsCount", result.getOrphanedDbRecords().size());
            status.put("orphanedMinioFilesCount", result.getOrphanedMinioFiles().size());
            status.put("inconsistentRecordsCount", result.getInconsistentRecords().size());
            
            return ApiResponse.success("数据一致性状态查询成功", status);
            
        } catch (Exception e) {
            log.error("查询数据一致性状态失败", e);
            return ApiResponse.error(500, "查询数据一致性状态失败: " + e.getMessage());
        }
    }
}