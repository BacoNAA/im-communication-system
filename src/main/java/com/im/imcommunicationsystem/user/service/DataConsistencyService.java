package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.impl.MinioServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据一致性服务
 * 用于检测和修复数据库与MinIO之间的数据不一致问题
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataConsistencyService {

    private final FileUploadRepository fileUploadRepository;
    private final MinioServiceImpl minioService;

    /**
     * 数据一致性检查结果
     */
    public static class ConsistencyCheckResult {
        private final List<String> orphanedDbRecords = new ArrayList<>();  // 数据库有记录但MinIO无文件
        private final List<String> orphanedMinioFiles = new ArrayList<>(); // MinIO有文件但数据库无记录
        private final List<String> inconsistentRecords = new ArrayList<>(); // 其他不一致情况
        
        public List<String> getOrphanedDbRecords() { return orphanedDbRecords; }
        public List<String> getOrphanedMinioFiles() { return orphanedMinioFiles; }
        public List<String> getInconsistentRecords() { return inconsistentRecords; }
        
        public boolean hasInconsistencies() {
            return !orphanedDbRecords.isEmpty() || !orphanedMinioFiles.isEmpty() || !inconsistentRecords.isEmpty();
        }
        
        public int getTotalInconsistencies() {
            return orphanedDbRecords.size() + orphanedMinioFiles.size() + inconsistentRecords.size();
        }
    }

    /**
     * 检查数据库与MinIO的数据一致性
     * @return 一致性检查结果
     */
    public ConsistencyCheckResult checkDataConsistency() {
        log.info("开始执行数据一致性检查");
        ConsistencyCheckResult result = new ConsistencyCheckResult();
        
        try {
            // 获取所有未删除的文件记录
            List<FileUpload> allFiles = fileUploadRepository.findAll().stream()
                .filter(file -> !file.getIsDeleted())
                .collect(Collectors.toList());
            
            for (FileUpload fileUpload : allFiles) {
                try {
                    // 检查MinIO中是否存在对应文件
                    boolean existsInMinio = minioService.fileExists(fileUpload.getBucketName(), fileUpload.getObjectKey());
                    
                    if (!existsInMinio) {
                        // 数据库有记录但MinIO无文件
                        String record = String.format("文件ID: %d, 文件名: %s, 对象键: %s", 
                            fileUpload.getId(), fileUpload.getOriginalName(), fileUpload.getObjectKey());
                        result.orphanedDbRecords.add(record);
                        log.warn("发现孤立数据库记录: {}", record);
                    }
                    
                    // 检查缩略图一致性（如果存在）
                    if (fileUpload.getThumbnailUrl() != null) {
                        String thumbnailObjectKey = extractObjectKeyFromUrl(fileUpload.getThumbnailUrl());
                        boolean thumbnailExists = minioService.fileExists(fileUpload.getBucketName(), thumbnailObjectKey);
                        
                        if (!thumbnailExists) {
                            String record = String.format("文件ID: %d, 缩略图对象键: %s", 
                                fileUpload.getId(), thumbnailObjectKey);
                            result.inconsistentRecords.add("缺失缩略图: " + record);
                            log.warn("发现缺失缩略图: {}", record);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("检查文件一致性时发生错误，文件ID: {}", fileUpload.getId(), e);
                    result.inconsistentRecords.add("检查失败: 文件ID " + fileUpload.getId() + ", 错误: " + e.getMessage());
                }
            }
            
            log.info("数据一致性检查完成，发现 {} 个不一致项", result.getTotalInconsistencies());
            
        } catch (Exception e) {
            log.error("数据一致性检查失败", e);
            result.inconsistentRecords.add("检查过程失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 修复数据一致性问题
     * @param repairOrphanedDbRecords 是否修复孤立的数据库记录（将其标记为已删除）
     * @return 修复结果统计
     */
    @Transactional
    public RepairResult repairDataConsistency(boolean repairOrphanedDbRecords) {
        log.info("开始执行数据一致性修复，修复孤立数据库记录: {}", repairOrphanedDbRecords);
        
        RepairResult repairResult = new RepairResult();
        ConsistencyCheckResult checkResult = checkDataConsistency();
        
        if (repairOrphanedDbRecords) {
            // 修复孤立的数据库记录（标记为已删除）
            List<FileUpload> allFiles = fileUploadRepository.findAll().stream()
                .filter(file -> !file.getIsDeleted())
                .collect(Collectors.toList());
            
            for (FileUpload fileUpload : allFiles) {
                try {
                    boolean existsInMinio = minioService.fileExists(fileUpload.getBucketName(), fileUpload.getObjectKey());
                    
                    if (!existsInMinio) {
                        // MinIO中不存在文件，将数据库记录标记为已删除
                        fileUpload.setIsDeleted(true);
                        fileUpload.setDeletedAt(java.time.LocalDateTime.now());
                        fileUploadRepository.save(fileUpload);
                        
                        repairResult.repairedDbRecords++;
                        log.info("修复孤立数据库记录，文件ID: {}, 文件名: {}", 
                            fileUpload.getId(), fileUpload.getOriginalName());
                    }
                    
                } catch (Exception e) {
                    log.error("修复文件记录时发生错误，文件ID: {}", fileUpload.getId(), e);
                    repairResult.failedRepairs++;
                }
            }
        }
        
        log.info("数据一致性修复完成，修复了 {} 个数据库记录，失败 {} 个", 
            repairResult.repairedDbRecords, repairResult.failedRepairs);
        
        return repairResult;
    }

    /**
     * 修复结果统计
     */
    public static class RepairResult {
        private int repairedDbRecords = 0;
        private int failedRepairs = 0;
        
        public int getRepairedDbRecords() { return repairedDbRecords; }
        public int getFailedRepairs() { return failedRepairs; }
        public int getTotalRepaired() { return repairedDbRecords; }
    }

    /**
     * 检查数据库记录是否存在
     */
    public boolean checkDatabaseRecordExists(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            return fileUploadRepository.findById(id).isPresent();
        } catch (Exception e) {
            log.error("检查数据库记录存在性失败: fileId={}", fileId, e);
            return false;
        }
    }

    /**
     * 检查数据库记录是否已删除
     */
    public boolean checkDatabaseRecordDeleted(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            return fileUploadRepository.findById(id)
                    .map(FileUpload::getIsDeleted)
                    .orElse(true);
        } catch (Exception e) {
            log.error("检查数据库记录删除状态失败: fileId={}", fileId, e);
            return true;
        }
    }

    /**
     * 检查MinIO文件是否存在
     */
    public boolean checkMinioFileExists(String bucketName, String objectKey) {
        try {
            return minioService.fileExists(bucketName, objectKey);
        } catch (Exception e) {
            log.error("检查MinIO文件存在性失败: bucketName={}, objectKey={}", bucketName, objectKey, e);
            return false;
        }
    }

    /**
     * 检查MinIO文件是否存在（兼容旧版本方法）
     * @deprecated 请使用 checkMinioFileExists(String bucketName, String objectKey)
     */
    @Deprecated
    public boolean checkMinioFileExists(String objectKey) {
        try {
            // 从objectKey中提取bucket和实际的key
            String[] parts = objectKey.split("/", 2);
            String bucketName = parts.length > 1 ? parts[0] : "files";
            String actualKey = parts.length > 1 ? parts[1] : objectKey;
            return minioService.fileExists(bucketName, actualKey);
        } catch (Exception e) {
            log.error("检查MinIO文件存在性失败: objectKey={}", objectKey, e);
            return false;
        }
    }

    /**
     * 标记数据库记录为已删除
     */
    @Transactional
    public void markDatabaseRecordAsDeleted(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            fileUploadRepository.findById(id).ifPresent(fileUpload -> {
                fileUpload.setIsDeleted(true);
                fileUpload.setDeletedAt(java.time.LocalDateTime.now());
                fileUploadRepository.save(fileUpload);
                log.info("已标记数据库记录为删除: fileId={}", fileId);
            });
        } catch (Exception e) {
            log.error("标记数据库记录删除失败: fileId={}", fileId, e);
        }
    }

    /**
     * 强制删除数据库记录
     */
    @Transactional
    public void forceDeleteDatabaseRecord(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            fileUploadRepository.deleteById(id);
            log.info("已强制删除数据库记录: fileId={}", fileId);
        } catch (Exception e) {
            log.error("强制删除数据库记录失败: fileId={}", fileId, e);
        }
    }

    /**
     * 强制删除MinIO文件
     */
    public void forceDeleteMinioFile(String objectKey) {
        try {
            // 从objectKey中提取bucket和实际的key
            String[] parts = objectKey.split("/", 2);
            String bucketName = parts.length > 1 ? parts[0] : "files";
            String actualKey = parts.length > 1 ? parts[1] : objectKey;
            minioService.deleteFile(bucketName, actualKey);
            log.info("已强制删除MinIO文件: objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("强制删除MinIO文件失败: objectKey={}", objectKey, e);
        }
    }

    /**
     * 从URL中提取对象键
     */
    private String extractObjectKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 假设URL格式为: http://localhost:9000/bucket-name/object-key
        // 提取最后一个斜杠后的部分作为对象键
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        
        return url; // 如果无法解析，返回原URL
    }
}