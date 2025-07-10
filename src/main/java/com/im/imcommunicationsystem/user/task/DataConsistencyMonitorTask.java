package com.im.imcommunicationsystem.user.task;

import com.im.imcommunicationsystem.user.service.DataConsistencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据一致性监控定时任务
 * 定期检查数据库与MinIO之间的数据一致性
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-consistency.monitor.enabled", havingValue = "true", matchIfMissing = false)
public class DataConsistencyMonitorTask {

    private final DataConsistencyService dataConsistencyService;

    /**
     * 每天凌晨4点执行数据一致性检查
     * 检查数据库与MinIO的数据一致性，记录不一致问题
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void dailyConsistencyCheck() {
        log.info("开始执行每日数据一致性检查");
        
        try {
            DataConsistencyService.ConsistencyCheckResult result = dataConsistencyService.checkDataConsistency();
            
            if (result.hasInconsistencies()) {
                log.warn("每日数据一致性检查发现问题：");
                log.warn("- 孤立数据库记录: {} 个", result.getOrphanedDbRecords().size());
                log.warn("- 孤立MinIO文件: {} 个", result.getOrphanedMinioFiles().size());
                log.warn("- 其他不一致记录: {} 个", result.getInconsistentRecords().size());
                log.warn("总计不一致项: {} 个", result.getTotalInconsistencies());
                
                // 记录详细的不一致信息
                if (!result.getOrphanedDbRecords().isEmpty()) {
                    log.warn("孤立数据库记录详情:");
                    result.getOrphanedDbRecords().forEach(record -> log.warn("  - {}", record));
                }
                
                if (!result.getOrphanedMinioFiles().isEmpty()) {
                    log.warn("孤立MinIO文件详情:");
                    result.getOrphanedMinioFiles().forEach(file -> log.warn("  - {}", file));
                }
                
                if (!result.getInconsistentRecords().isEmpty()) {
                    log.warn("其他不一致记录详情:");
                    result.getInconsistentRecords().forEach(record -> log.warn("  - {}", record));
                }
                
            } else {
                log.info("每日数据一致性检查通过，未发现问题");
            }
            
        } catch (Exception e) {
            log.error("每日数据一致性检查失败", e);
        }
        
        log.info("每日数据一致性检查完成");
    }

    /**
     * 每天凌晨2点执行数据一致性检查并自动修复
     * 自动修复孤立的数据库记录（标记为已删除）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyConsistencyCheckAndRepair() {
        log.info("开始执行每天数据一致性检查和自动修复");
        
        try {
            // 先执行检查
            DataConsistencyService.ConsistencyCheckResult checkResult = dataConsistencyService.checkDataConsistency();
            
            if (checkResult.hasInconsistencies()) {
                log.info("发现数据不一致问题，开始自动修复");
                
                // 执行自动修复（只修复孤立的数据库记录）
                DataConsistencyService.RepairResult repairResult = dataConsistencyService.repairDataConsistency(true);
                
                log.info("每天数据一致性自动修复完成：");
                log.info("- 修复的数据库记录: {} 个", repairResult.getRepairedDbRecords());
                log.info("- 修复失败: {} 个", repairResult.getFailedRepairs());
                log.info("- 总计修复: {} 个", repairResult.getTotalRepaired());
                
                if (repairResult.getFailedRepairs() > 0) {
                    log.warn("部分修复操作失败，请手动检查");
                }
                
            } else {
                log.info("每天数据一致性检查通过，无需修复");
            }
            
        } catch (Exception e) {
            log.error("每天数据一致性检查和修复失败", e);
        }
        
        log.info("每天数据一致性检查和自动修复完成");
    }

    /**
     * 每小时执行一次快速一致性检查（仅统计）
     * 用于监控数据一致性状态变化
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyQuickCheck() {
        log.debug("开始执行每小时快速数据一致性检查");
        
        try {
            DataConsistencyService.ConsistencyCheckResult result = dataConsistencyService.checkDataConsistency();
            
            if (result.hasInconsistencies()) {
                log.info("快速检查发现数据不一致: {} 个问题", result.getTotalInconsistencies());
            } else {
                log.debug("快速检查通过，数据一致性正常");
            }
            
        } catch (Exception e) {
            log.error("每小时快速数据一致性检查失败", e);
        }
    }
}