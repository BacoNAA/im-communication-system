package com.im.imcommunicationsystem.user.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.user.service.MinioLifecycleService;
import com.im.imcommunicationsystem.user.task.TemporaryFileCleanupTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 临时文件管理控制器
 * 提供临时文件清理和MinIO生命周期规则管理的API接口
 */
@RestController
@RequestMapping("/api/admin/temporary-files")
@RequiredArgsConstructor
@Slf4j
public class TemporaryFileManagementController {
    
    private final TemporaryFileCleanupTask temporaryFileCleanupTask;
    private final MinioLifecycleService minioLifecycleService;
    
    /**
     * 手动触发过期临时文件清理
     */
    @PostMapping("/cleanup/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> cleanupExpiredTemporaryFiles() {
        try {
            log.info("管理员手动触发过期临时文件清理");
            temporaryFileCleanupTask.manualCleanupExpiredTemporaryFiles();
            return ApiResponse.success("过期临时文件清理任务已启动");
        } catch (Exception e) {
            log.error("手动清理过期临时文件失败", e);
            return ApiResponse.error(500, "清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发公共桶过期临时文件清理
     */
    @PostMapping("/cleanup/public-buckets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> cleanupPublicBucketTemporaryFiles() {
        try {
            log.info("管理员手动触发公共桶过期临时文件清理");
            temporaryFileCleanupTask.manualCleanupPublicBucketTemporaryFiles();
            return ApiResponse.success("公共桶过期临时文件清理任务已启动");
        } catch (Exception e) {
            log.error("手动清理公共桶过期临时文件失败", e);
            return ApiResponse.error(500, "清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 为指定桶设置临时文件生命周期规则
     */
    @PostMapping("/lifecycle/bucket/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> setLifecycleRule(
            @PathVariable String bucketName,
            @RequestParam(defaultValue = "7") int expirationDays) {
        try {
            log.info("管理员为桶 {} 设置生命周期规则，过期天数: {}", bucketName, expirationDays);
            
            boolean success = minioLifecycleService.setTemporaryFileLifecycleRule(bucketName, expirationDays);
            
            if (success) {
                return ApiResponse.success("生命周期规则设置成功");
            } else {
                return ApiResponse.error(500, "生命周期规则设置失败");
            }
        } catch (Exception e) {
            log.error("设置生命周期规则失败", e);
            return ApiResponse.error(500, "设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 为所有公共桶设置临时文件生命周期规则
     */
    @PostMapping("/lifecycle/public-buckets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> setLifecycleRuleForAllPublicBuckets(
            @RequestParam(defaultValue = "7") int expirationDays) {
        try {
            log.info("管理员为所有公共桶设置生命周期规则，过期天数: {}", expirationDays);
            
            int successCount = minioLifecycleService.setTemporaryFileLifecycleRuleForAllPublicBuckets(expirationDays);
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("expirationDays", expirationDays);
            result.put("message", "成功为 " + successCount + " 个公共桶设置生命周期规则");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("为所有公共桶设置生命周期规则失败", e);
            return ApiResponse.error(500, "设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除指定桶的生命周期规则
     */
    @DeleteMapping("/lifecycle/bucket/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> removeLifecycleRule(@PathVariable String bucketName) {
        try {
            log.info("管理员删除桶 {} 的生命周期规则", bucketName);
            
            boolean success = minioLifecycleService.removeLifecycleRule(bucketName);
            
            if (success) {
                return ApiResponse.success("生命周期规则删除成功");
            } else {
                return ApiResponse.error(500, "生命周期规则删除失败");
            }
        } catch (Exception e) {
            log.error("删除生命周期规则失败", e);
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定桶的生命周期规则
     */
    @GetMapping("/lifecycle/bucket/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getLifecycleRules(@PathVariable String bucketName) {
        try {
            String rules = minioLifecycleService.getLifecycleRules(bucketName);
            boolean hasRule = minioLifecycleService.hasLifecycleRule(bucketName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("bucketName", bucketName);
            result.put("hasRule", hasRule);
            result.put("rules", rules);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取生命周期规则失败", e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查指定桶是否已设置生命周期规则
     */
    @GetMapping("/lifecycle/bucket/{bucketName}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> checkLifecycleRuleStatus(@PathVariable String bucketName) {
        try {
            boolean hasRule = minioLifecycleService.hasLifecycleRule(bucketName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("bucketName", bucketName);
            result.put("hasLifecycleRule", hasRule);
            result.put("status", hasRule ? "已设置" : "未设置");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("检查生命周期规则状态失败", e);
            return ApiResponse.error(500, "检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新初始化所有公共桶的生命周期规则
     */
    @PostMapping("/lifecycle/reinitialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> reinitializeLifecycleRules() {
        try {
            log.info("管理员重新初始化所有公共桶的生命周期规则");
            minioLifecycleService.initializePublicBucketLifecycleRules();
            return ApiResponse.success("生命周期规则重新初始化完成");
        } catch (Exception e) {
            log.error("重新初始化生命周期规则失败", e);
            return ApiResponse.error(500, "初始化失败: " + e.getMessage());
        }
    }
}