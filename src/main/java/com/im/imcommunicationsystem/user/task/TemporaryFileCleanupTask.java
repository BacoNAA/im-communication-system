package com.im.imcommunicationsystem.user.task;

import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 临时文件清理定时任务
 * 定期清理过期的临时文件，释放存储空间
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.file.temporary-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class TemporaryFileCleanupTask {

    private final FileUploadRepository fileUploadRepository;
    private final MinioService minioService;

    /**
     * 每小时执行一次临时文件清理任务
     * 清理已过期的临时文件
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredTemporaryFiles() {
        log.info("开始执行过期临时文件清理任务");
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            
            // 查找所有过期的临时文件
            List<FileUpload> expiredFiles = fileUploadRepository.findExpiredTemporaryFiles(currentTime);
            
            if (expiredFiles.isEmpty()) {
                log.info("没有发现过期的临时文件");
                return;
            }
            
            log.info("发现 {} 个过期的临时文件，开始清理", expiredFiles.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (FileUpload file : expiredFiles) {
                try {
                    // 从MinIO删除文件
                    if (file.getBucketName() != null && file.getObjectKey() != null) {
                        minioService.deleteFile(file.getBucketName(), file.getObjectKey());
                        log.debug("已从MinIO删除过期临时文件: bucket={}, key={}", 
                                file.getBucketName(), file.getObjectKey());
                    }
                    
                    // 从数据库物理删除记录
                    fileUploadRepository.delete(file);
                    
                    successCount++;
                    log.debug("已清理过期临时文件: id={}, fileName={}, expiresAt={}", 
                            file.getId(), file.getFileName(), file.getExpiresAt());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("清理过期临时文件失败: id={}, fileName={}, error={}", 
                            file.getId(), file.getFileName(), e.getMessage(), e);
                }
            }
            
            log.info("过期临时文件清理任务完成，成功: {}, 失败: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("执行过期临时文件清理任务时发生错误", e);
        }
    }
    
    /**
     * 每天凌晨3点执行公共桶临时文件清理任务
     * 专门清理公共桶中的过期临时文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupPublicBucketTemporaryFiles() {
        log.info("开始执行公共桶过期临时文件清理任务");
        
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            
            // 清理各个公共桶中的过期临时文件
            String[] publicBuckets = {"public-files", "public-images", "public-videos", "public-documents"};
            
            int totalCleaned = 0;
            
            for (String bucketName : publicBuckets) {
                List<FileUpload> expiredFiles = fileUploadRepository
                        .findExpiredTemporaryFilesByBucket(bucketName, currentTime);
                
                if (!expiredFiles.isEmpty()) {
                    log.info("在桶 {} 中发现 {} 个过期的临时文件", bucketName, expiredFiles.size());
                    
                    for (FileUpload file : expiredFiles) {
                        try {
                            // 从MinIO删除文件
                            minioService.deleteFile(file.getBucketName(), file.getObjectKey());
                            
                            // 从数据库物理删除记录
                            fileUploadRepository.delete(file);
                            
                            totalCleaned++;
                            log.debug("已清理公共桶过期临时文件: bucket={}, key={}, expiresAt={}", 
                                    file.getBucketName(), file.getObjectKey(), file.getExpiresAt());
                            
                        } catch (Exception e) {
                            log.error("清理公共桶过期临时文件失败: bucket={}, key={}, error={}", 
                                    file.getBucketName(), file.getObjectKey(), e.getMessage(), e);
                        }
                    }
                }
            }
            
            log.info("公共桶过期临时文件清理任务完成，总计清理: {} 个文件", totalCleaned);
            
        } catch (Exception e) {
            log.error("执行公共桶过期临时文件清理任务时发生错误", e);
        }
    }
    
    /**
     * 手动清理过期临时文件
     * 提供给管理员手动触发清理的接口
     */
    public void manualCleanupExpiredTemporaryFiles() {
        log.info("手动触发过期临时文件清理任务");
        cleanupExpiredTemporaryFiles();
    }
    
    /**
     * 手动清理公共桶过期临时文件
     * 提供给管理员手动触发清理的接口
     */
    public void manualCleanupPublicBucketTemporaryFiles() {
        log.info("手动触发公共桶过期临时文件清理任务");
        cleanupPublicBucketTemporaryFiles();
    }
}