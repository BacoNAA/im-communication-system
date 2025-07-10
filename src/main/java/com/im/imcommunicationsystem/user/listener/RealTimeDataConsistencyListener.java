package com.im.imcommunicationsystem.user.listener;

import com.im.imcommunicationsystem.user.event.FileOperationEvent;
import com.im.imcommunicationsystem.user.service.DataConsistencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 实时数据一致性监听器
 * 监听文件操作事件，自动执行数据一致性检查和修复
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-consistency.real-time.enabled", havingValue = "true", matchIfMissing = true)
public class RealTimeDataConsistencyListener {

    private final DataConsistencyService dataConsistencyService;

    /**
     * 监听文件操作事件，异步执行数据一致性检查
     */
    @EventListener
    @Async("dataConsistencyExecutor")
    public void handleFileOperationEvent(FileOperationEvent event) {
        log.debug("接收到文件操作事件: {}", event);
        
        try {
            // 延迟一小段时间，确保数据库事务已提交
            Thread.sleep(500);
            
            // 执行针对特定文件的一致性检查
            checkSpecificFileConsistency(event);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("数据一致性检查被中断: {}", event.getFileId());
        } catch (Exception e) {
            log.error("实时数据一致性检查失败: {}", event, e);
        }
    }

    /**
     * 检查特定文件的数据一致性
     */
    private void checkSpecificFileConsistency(FileOperationEvent event) {
        String fileId = event.getFileId();
        String objectKey = event.getObjectKey();
        
        try {
            switch (event.getOperationType()) {
                case UPLOAD:
                    // 验证上传后的一致性
                    validateUploadConsistency(fileId, objectKey);
                    break;
                    
                case DELETE:
                    // 验证软删除后的一致性
                    validateSoftDeleteConsistency(fileId);
                    break;
                    
                case PHYSICAL_DELETE:
                    // 验证物理删除后的一致性
                    validatePhysicalDeleteConsistency(fileId, objectKey);
                    break;
                    
                case RESTORE:
                    // 验证恢复后的一致性
                    validateRestoreConsistency(fileId, objectKey);
                    break;
                    
                default:
                    log.warn("未知的文件操作类型: {}", event.getOperationType());
            }
            
        } catch (Exception e) {
            log.error("特定文件一致性检查失败: fileId={}, operation={}", 
                     fileId, event.getOperationType(), e);
        }
    }

    /**
     * 验证文件上传后的一致性
     */
    private void validateUploadConsistency(String fileId, String objectKey) {
        log.debug("验证文件上传一致性: fileId={}, objectKey={}", fileId, objectKey);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 检查数据库记录是否存在
                boolean dbExists = dataConsistencyService.checkDatabaseRecordExists(fileId);
                
                // 暂时禁用MinIO文件存在性检查，避免误删文件
                // TODO: 需要从数据库获取正确的bucketName后再启用
                log.debug("跳过MinIO文件存在性检查，避免误删文件: fileId={}", fileId);
                
                if (!dbExists) {
                    log.warn("文件上传后数据库记录不存在: fileId={}", fileId);
                } else {
                    log.debug("文件上传一致性验证通过: fileId={}", fileId);
                }
                
            } catch (Exception e) {
                log.error("验证上传一致性失败: fileId={}", fileId, e);
            }
        });
    }

    /**
     * 验证软删除后的一致性
     */
    private void validateSoftDeleteConsistency(String fileId) {
        log.debug("验证软删除一致性: fileId={}", fileId);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 软删除只影响数据库，MinIO文件应该仍然存在
                boolean dbDeleted = dataConsistencyService.checkDatabaseRecordDeleted(fileId);
                
                if (!dbDeleted) {
                    log.warn("软删除后数据库记录未正确标记: fileId={}", fileId);
                } else {
                    log.debug("软删除一致性验证通过: fileId={}", fileId);
                }
                
            } catch (Exception e) {
                log.error("验证软删除一致性失败: fileId={}", fileId, e);
            }
        });
    }

    /**
     * 验证物理删除后的一致性
     */
    private void validatePhysicalDeleteConsistency(String fileId, String objectKey) {
        log.debug("验证物理删除一致性: fileId={}, objectKey={}", fileId, objectKey);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 物理删除后，数据库记录和MinIO文件都应该不存在
                boolean dbExists = dataConsistencyService.checkDatabaseRecordExists(fileId);
                boolean minioExists = dataConsistencyService.checkMinioFileExists(objectKey);
                
                if (dbExists || minioExists) {
                    log.warn("物理删除后数据不一致: fileId={}, dbExists={}, minioExists={}", 
                            fileId, dbExists, minioExists);
                    
                    // 尝试自动修复
                    if (dbExists) {
                        log.info("检测到数据库记录残留，删除记录: fileId={}", fileId);
                        dataConsistencyService.forceDeleteDatabaseRecord(fileId);
                    }
                    if (minioExists) {
                        log.info("检测到MinIO文件残留，删除文件: objectKey={}", objectKey);
                        dataConsistencyService.forceDeleteMinioFile(objectKey);
                    }
                } else {
                    log.debug("物理删除一致性验证通过: fileId={}", fileId);
                }
                
            } catch (Exception e) {
                log.error("验证物理删除一致性失败: fileId={}", fileId, e);
            }
        });
    }

    /**
     * 验证文件恢复后的一致性
     */
    private void validateRestoreConsistency(String fileId, String objectKey) {
        log.debug("验证文件恢复一致性: fileId={}, objectKey={}", fileId, objectKey);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 恢复后，数据库记录应该存在且未删除，MinIO文件也应该存在
                boolean dbExists = dataConsistencyService.checkDatabaseRecordExists(fileId);
                boolean dbDeleted = dataConsistencyService.checkDatabaseRecordDeleted(fileId);
                boolean minioExists = dataConsistencyService.checkMinioFileExists(objectKey);
                
                if (!dbExists || dbDeleted || !minioExists) {
                    log.warn("文件恢复后数据不一致: fileId={}, dbExists={}, dbDeleted={}, minioExists={}", 
                            fileId, dbExists, dbDeleted, minioExists);
                    
                    // 尝试自动修复
                    if (!minioExists) {
                        log.info("检测到MinIO文件缺失，标记数据库记录为已删除: fileId={}", fileId);
                        dataConsistencyService.markDatabaseRecordAsDeleted(fileId);
                    }
                } else {
                    log.debug("文件恢复一致性验证通过: fileId={}", fileId);
                }
                
            } catch (Exception e) {
                log.error("验证恢复一致性失败: fileId={}", fileId, e);
            }
        });
    }
}