package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.user.config.MinioConfig;
import com.im.imcommunicationsystem.user.service.MinioService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * MinIO对象存储服务实现类
 * 实现文件上传、下载、删除等基础操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 服务初始化后自动创建必要的存储桶
     */
    @PostConstruct
    public void init() {
        log.info("初始化MinIO服务，开始创建存储桶...");
        initializeBuckets();
        log.info("MinIO服务初始化完成");
    }

    @Override
    public boolean createBucketIfNotExists(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建存储桶成功: {}", bucketName);
                return true;
            }
            log.debug("存储桶已存在: {}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("创建存储桶失败: {}", bucketName, e);
            return false;
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("检查存储桶是否存在失败: {}", bucketName, e);
            return false;
        }
    }

    @Override
    public boolean uploadFile(String bucketName, String objectKey, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                log.warn("上传文件为空: {}", objectKey);
                return false;
            }

            // 确保存储桶存在
            if (!createBucketIfNotExists(bucketName)) {
                log.error("存储桶创建失败，无法上传文件: {}", bucketName);
                return false;
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("文件上传成功: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件上传失败: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean uploadFile(String bucketName, String objectKey, InputStream inputStream, String contentType, long size) {
        try {
            // 确保存储桶存在
            if (!createBucketIfNotExists(bucketName)) {
                log.error("存储桶创建失败，无法上传文件: {}", bucketName);
                return false;
            }

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            log.info("文件流上传成功: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件流上传失败: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean uploadFileWithMetadata(String bucketName, String objectKey, MultipartFile file, Map<String, String> metadata) {
        try {
            if (file.isEmpty()) {
                log.warn("上传文件为空: {}", objectKey);
                return false;
            }

            // 确保存储桶存在
            if (!createBucketIfNotExists(bucketName)) {
                log.error("存储桶创建失败，无法上传文件: {}", bucketName);
                return false;
            }

            // 构建上传参数
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType());

            // 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                builder.userMetadata(metadata);
            }

            // 上传文件
            minioClient.putObject(builder.build());

            log.info("文件上传成功（含元数据）: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件上传失败（含元数据）: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public InputStream downloadFile(String bucketName, String objectKey) {
        try {
            log.info("开始从MinIO下载文件: bucketName={}, objectKey={}", bucketName, objectKey);
            
            // 检查参数
            if (bucketName == null || bucketName.trim().isEmpty()) {
                log.error("下载文件失败: bucketName为空");
                return null;
            }
            
            if (objectKey == null || objectKey.trim().isEmpty()) {
                log.error("下载文件失败: objectKey为空");
                return null;
            }
            
            // 检查存储桶是否存在
            boolean bucketExists = bucketExists(bucketName);
            if (!bucketExists) {
                log.error("下载文件失败: 存储桶不存在 - {}", bucketName);
                return null;
            }
            
            // 检查文件是否存在
            boolean fileExists = fileExists(bucketName, objectKey);
            if (!fileExists) {
                log.error("下载文件失败: 文件不存在 - {}/{}", bucketName, objectKey);
                return null;
            }
            
            // 获取文件
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            
            log.info("文件下载成功: {}/{}", bucketName, objectKey);
            return stream;
            
        } catch (Exception e) {
            log.error("文件下载失败: {}/{} - {}", bucketName, objectKey, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String bucketName, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            log.info("文件删除成功: {}/{}", bucketName, objectKey);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean fileExists(String bucketName, String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            log.debug("文件不存在或检查失败: {}/{}", bucketName, objectKey);
            return false;
        }
    }

    @Override
    public Map<String, Object> getFileInfo(String bucketName, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());

            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("bucketName", bucketName);
            fileInfo.put("objectKey", objectKey);
            fileInfo.put("size", stat.size());
            fileInfo.put("contentType", stat.contentType());
            fileInfo.put("etag", stat.etag());
            fileInfo.put("lastModified", stat.lastModified());
            fileInfo.put("userMetadata", stat.userMetadata());

            return fileInfo;
        } catch (Exception e) {
            log.error("获取文件信息失败: {}/{}", bucketName, objectKey, e);
            return null;
        }
    }

    @Override
    public String getPresignedUploadUrl(String bucketName, String objectKey, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expireSeconds)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名上传URL失败: {}/{}", bucketName, objectKey, e);
            return null;
        }
    }

    @Override
    public String getPresignedDownloadUrl(String bucketName, String objectKey, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expireSeconds)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名下载URL失败: {}/{}", bucketName, objectKey, e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String bucketName, String objectKey) {
        return minioConfig.getFileUrl(bucketName, objectKey);
    }

    @Override
    public boolean copyFile(String sourceBucket, String sourceObjectKey, String targetBucket, String targetObjectKey) {
        try {
            // 确保目标存储桶存在
            if (!createBucketIfNotExists(targetBucket)) {
                log.error("目标存储桶创建失败，无法复制文件: {}", targetBucket);
                return false;
            }

            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(targetObjectKey)
                    .source(CopySource.builder()
                            .bucket(sourceBucket)
                            .object(sourceObjectKey)
                            .build())
                    .build());

            log.info("文件复制成功: {}/{} -> {}/{}", sourceBucket, sourceObjectKey, targetBucket, targetObjectKey);
            return true;

        } catch (Exception e) {
            log.error("文件复制失败: {}/{} -> {}/{}", sourceBucket, sourceObjectKey, targetBucket, targetObjectKey, e);
            return false;
        }
    }

    @Override
    public boolean uploadFileWithTags(String bucketName, String objectKey, MultipartFile file, Map<String, String> tags) {
        try {
            if (file.isEmpty()) {
                log.warn("上传文件为空: {}", objectKey);
                return false;
            }

            // 确保存储桶存在
            if (!createBucketIfNotExists(bucketName)) {
                log.error("存储桶创建失败，无法上传文件: {}", bucketName);
                return false;
            }

            // 构建上传参数
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType());

            // 添加标签
            if (tags != null && !tags.isEmpty()) {
                builder.tags(tags);
            }

            // 上传文件
            minioClient.putObject(builder.build());

            log.info("文件上传成功（含标签）: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件上传失败（含标签）: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean uploadFileWithMetadataAndTags(String bucketName, String objectKey, MultipartFile file, 
                                                Map<String, String> metadata, Map<String, String> tags) {
        try {
            if (file.isEmpty()) {
                log.warn("上传文件为空: {}", objectKey);
                return false;
            }

            // 确保存储桶存在
            if (!createBucketIfNotExists(bucketName)) {
                log.error("存储桶创建失败，无法上传文件: {}", bucketName);
                return false;
            }

            // 构建上传参数
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType());

            // 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                builder.userMetadata(metadata);
            }

            // 添加标签
            if (tags != null && !tags.isEmpty()) {
                builder.tags(tags);
            }

            // 上传文件
            minioClient.putObject(builder.build());

            log.info("文件上传成功（含元数据和标签）: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件上传失败（含元数据和标签）: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public boolean setObjectTags(String bucketName, String objectKey, Map<String, String> tags) {
        try {
            minioClient.setObjectTags(SetObjectTagsArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .tags(tags)
                    .build());

            log.info("文件标签设置成功: {}/{}", bucketName, objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件标签设置失败: {}/{}", bucketName, objectKey, e);
            return false;
        }
    }

    @Override
    public Map<String, String> getObjectTags(String bucketName, String objectKey) {
        try {
            Tags tags = minioClient.getObjectTags(GetObjectTagsArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());

            return tags.get();

        } catch (Exception e) {
            log.error("获取文件标签失败: {}/{}", bucketName, objectKey, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean initializeBuckets() {
        try {
            String[] bucketNames = minioConfig.getAllBucketNames();
            boolean allSuccess = true;

            for (String bucketName : bucketNames) {
                if (!createBucketIfNotExists(bucketName)) {
                    allSuccess = false;
                    log.error("存储桶创建失败: {}", bucketName);
                }
            }

            if (allSuccess) {
                log.info("所有存储桶初始化成功");
            } else {
                log.warn("部分存储桶初始化失败");
            }

            return allSuccess;

        } catch (Exception e) {
            log.error("存储桶初始化过程中发生异常", e);
            return false;
        }
    }
}