package com.im.imcommunicationsystem.user.task;

import com.im.imcommunicationsystem.user.service.impl.MinioFileUploadServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件清理定时任务
 * 定期清理过期的已删除文件，保持数据库与MinIO的数据一致性
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.file.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class FileCleanupTask {

    private final MinioFileUploadServiceImpl fileUploadService;
    
    @Value("${app.file.cleanup.daily-cleanup-days:30}")
    private int dailyCleanupDays;
    
    @Value("${app.file.cleanup.weekly-cleanup-days:90}")
    private int weeklyCleanupDays;

    /**
     * 每天凌晨2点执行文件清理任务
     * 清理配置天数前软删除的文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredFiles() {
        log.info("开始执行定时文件清理任务，清理{}天前的文件", dailyCleanupDays);
        
        try {
            int deletedCount = fileUploadService.cleanupExpiredFiles(dailyCleanupDays);
            log.info("定时文件清理任务完成，清理了 {} 个过期文件", deletedCount);
            
        } catch (Exception e) {
            log.error("定时文件清理任务执行失败", e);
        }
    }

    /**
     * 每周日凌晨3点执行深度清理任务
     * 清理更长时间前软删除的文件
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deepCleanupExpiredFiles() {
        log.info("开始执行深度文件清理任务，清理{}天前的文件", weeklyCleanupDays);
        
        try {
            int deletedCount = fileUploadService.cleanupExpiredFiles(weeklyCleanupDays);
            log.info("深度文件清理任务完成，清理了 {} 个过期文件", deletedCount);
            
        } catch (Exception e) {
            log.error("深度文件清理任务执行失败", e);
        }
    }
}