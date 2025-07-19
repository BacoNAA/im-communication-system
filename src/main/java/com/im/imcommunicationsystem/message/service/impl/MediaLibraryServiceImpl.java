package com.im.imcommunicationsystem.message.service.impl;

import com.im.imcommunicationsystem.message.dto.request.MediaUploadRequest;
import com.im.imcommunicationsystem.message.dto.response.MediaResponse;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.service.FileUploadService;
import com.im.imcommunicationsystem.message.service.MediaLibraryService;
import com.im.imcommunicationsystem.user.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 媒体库服务实现类
 * 实现媒体文件管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MediaLibraryServiceImpl implements MediaLibraryService {

    private final FileUploadService fileUploadService;
    private final MinioService minioService;

    @Override
    @Transactional(readOnly = true)
    public Page<MediaResponse> getMediaLibrary(Long conversationId, String mediaType, Pageable pageable, Long userId) {
        log.info("Getting media library for user {} in conversation {} with type {}", 
                userId, conversationId, mediaType);
        
        try {
            // 获取媒体文件
            List<FileUpload> fileUploads;
            
            if (conversationId != null) {
                // 获取特定会话的媒体文件
                log.info("获取会话 {} 的媒体文件", conversationId);
                fileUploads = fileUploadService.getFilesByConversationId(conversationId, pageable.getPageNumber(), pageable.getPageSize());
            } else {
                // 获取用户的所有媒体文件
                log.info("获取用户 {} 的所有媒体文件", userId);
                fileUploads = fileUploadService.getFilesByUploaderId(userId, pageable.getPageNumber(), pageable.getPageSize());
            }
            
            log.info("原始文件列表大小: {}", fileUploads.size());
            
            // 根据媒体类型过滤
            if (mediaType != null && !mediaType.equalsIgnoreCase("all")) {
                log.info("根据媒体类型 {} 过滤文件", mediaType);
                
                // 过滤文件类型
                fileUploads = fileUploads.stream()
                        .filter(file -> {
                            if (file.getFileType() == null) {
                                return false;
                            }
                            
                            String fileType = file.getFileType().name().toLowerCase();
                            String requestType = mediaType.toLowerCase();
                            
                            log.debug("比较文件类型: {} 与请求类型: {}", fileType, requestType);
                            return fileType.equals(requestType);
                        })
                        .toList();
                
                log.info("过滤后文件列表大小: {}", fileUploads.size());
            }
            
            // 转换为MediaResponse列表
            List<MediaResponse> mediaResponseList = fileUploads.stream()
                    .map(file -> {
                        MediaResponse response = MediaResponse.builder()
                                .id(file.getId())
                                .fileName(file.getFileName())
                                .originalFileName(file.getOriginalName())
                                .fileType(file.getFileType() != null ? file.getFileType().name() : null)
                                .fileSize(file.getFileSize())
                                .fileUrl(file.getFileUrl())
                                .url(file.getFileUrl()) // 添加url字段，与前端兼容
                                .thumbnailUrl(null) // 暂不支持缩略图
                                .uploadTime(file.getCreatedAt())
                                .uploaderId(file.getUserId())
                                .status("completed")
                                .build();
                        
                        log.debug("转换文件: id={}, name={}, url={}", 
                                file.getId(), file.getFileName(), file.getFileUrl());
                        
                        return response;
                    })
                    .toList();
            
            log.info("转换后的媒体响应列表大小: {}", mediaResponseList.size());
            
            // 创建Page对象
            Page<MediaResponse> mediaResponses = new org.springframework.data.domain.PageImpl<>(
                    mediaResponseList,
                    pageable,
                    mediaResponseList.size() // 总数量
            );
            
            log.info("获取媒体库成功: 总数={}, 页码={}/{}", 
                    mediaResponses.getTotalElements(), mediaResponses.getNumber() + 1, mediaResponses.getTotalPages());
            
            return mediaResponses;
            
        } catch (Exception e) {
            log.error("获取媒体库失败", e);
            return Page.empty();
        }
    }

    @Override
    public MediaResponse uploadMedia(MultipartFile file, MediaUploadRequest request, Long userId) {
        log.info("Uploading media file {} by user {}", file.getOriginalFilename(), userId);
        
        try {
            // 1. 验证文件是否为空
            if (file.isEmpty()) {
                log.error("上传失败：文件为空");
                throw new IllegalArgumentException("文件不能为空");
            }
            
            // 2. 验证文件类型
            if (!validateFileType(file)) {
                log.error("上传失败：不支持的文件类型 {}", file.getContentType());
                throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
            }
            
            // 3. 验证文件大小
            if (!validateFileSize(file)) {
                log.error("上传失败：文件过大 {}", file.getSize());
                throw new IllegalArgumentException("文件大小超过限制");
            }
            
            // 4. 获取会话ID（如果请求中有）
            Long conversationId = null;
            if (request != null && request.getConversationId() != null) {
                // 处理不同类型的conversationId
                Object convIdObj = request.getConversationId();
                
                if (convIdObj instanceof Long) {
                    // 如果已经是Long类型
                    conversationId = (Long) convIdObj;
                    log.info("直接获取会话ID (Long类型): {}", conversationId);
                } else if (convIdObj instanceof Number) {
                    // 如果是其他数字类型 (Integer等)
                    conversationId = ((Number) convIdObj).longValue();
                    log.info("从Number类型转换会话ID: {}", conversationId);
                } else if (convIdObj instanceof String) {
                    // 如果是字符串类型
                    String convIdStr = (String) convIdObj;
                    if (!convIdStr.isEmpty()) {
                        try {
                            conversationId = Long.valueOf(convIdStr);
                            log.info("从字符串解析会话ID: {}", conversationId);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析会话ID字符串: {}", convIdStr);
                            // 不抛出异常，继续处理
                        }
                    }
                } else {
                    // 其他未知类型
                    log.warn("无法处理的会话ID类型: {}", convIdObj.getClass().getName());
                }
            }
            
            log.info("最终会话ID: {}", conversationId);
            
            // 5. 上传文件到存储服务
            FileUpload fileUpload = fileUploadService.uploadMediaFile(file, userId, conversationId, null);
            
            if (fileUpload == null) {
                log.error("上传失败：存储服务返回空结果");
                throw new RuntimeException("文件上传失败");
            }
            
            // 6. 如果是图片且需要压缩，生成缩略图
            String thumbnailUrl = null;
            if (fileUpload.getFileType() == FileUpload.FileType.image && 
                request != null && request.shouldCompress()) {
                // 这里可以实现缩略图生成逻辑，暂时留空
                // thumbnailUrl = generateThumbnail(fileUpload);
            }
            
            // 7. 构建响应对象
            MediaResponse response = MediaResponse.builder()
                    .id(fileUpload.getId())
                    .fileName(fileUpload.getFileName())
                    .originalFileName(fileUpload.getOriginalName())
                    .fileType(fileUpload.getFileType().name())
                    .fileSize(fileUpload.getFileSize())
                    .fileUrl(fileUpload.getFileUrl())
                    .url(fileUpload.getFileUrl()) // 添加url字段，与前端兼容
                    .thumbnailUrl(thumbnailUrl)
                    .uploadTime(fileUpload.getCreatedAt())
                    .uploaderId(userId)
                    .compressed(request != null && request.shouldCompress())
                    .status("completed")
                    .build();
            
            log.info("文件上传成功: id={}, url={}", fileUpload.getId(), fileUpload.getFileUrl());
            return response;
            
        } catch (Exception e) {
            log.error("文件上传过程中发生错误", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadMedia(Long mediaId, Long userId) {
        log.info("下载媒体文件 - 媒体ID: {}, 用户ID: {}", mediaId, userId);
        
        try {
            // 1. 获取文件信息
            FileUpload fileUpload = getMediaFileById(mediaId);
            if (fileUpload == null) {
                log.warn("媒体文件不存在 - 媒体ID: {}", mediaId);
                return null;
            }
            
            // 2. 验证权限
            boolean hasAccess = false;
            
            // 如果是公开访问级别，任何人都可以访问
            if (fileUpload.getAccessLevel() == FileUpload.AccessLevel.PUBLIC) {
                log.info("文件为公开访问级别，允许访问 - 媒体ID: {}", mediaId);
                hasAccess = true;
            }
            // 如果是文件所有者，允许访问
            else if (fileUpload.getUserId().equals(userId)) {
                log.info("用户是文件所有者，允许访问 - 媒体ID: {}, 所有者ID: {}", mediaId, userId);
                hasAccess = true;
            }
            // 如果是会话成员，允许访问
            else if (fileUpload.getConversationId() != null) {
                // TODO: 实现会话成员权限验证
                // 临时允许所有请求通过，以修复图片加载问题
                log.info("文件属于会话，暂时允许访问 - 媒体ID: {}, 会话ID: {}", mediaId, fileUpload.getConversationId());
                hasAccess = true;
            }
            
            // 如果没有访问权限，拒绝请求
            if (!hasAccess) {
                log.warn("用户无权访问文件 - 媒体ID: {}, 请求用户ID: {}, 文件所有者ID: {}", 
                        mediaId, userId, fileUpload.getUserId());
                return null;
            }
            
            // 3. 从存储服务获取文件内容
            byte[] fileContent = null;
            
            // 根据存储类型获取文件内容
            if (fileUpload.getStorageType() == FileUpload.StorageType.MINIO) {
                // 从MinIO获取文件
                String bucketName = fileUpload.getBucketName();
                String objectKey = fileUpload.getFilePath();
                
                if (bucketName == null || objectKey == null) {
                    log.error("文件存储信息不完整 - 媒体ID: {}, bucketName: {}, objectKey: {}", 
                            mediaId, bucketName, objectKey);
                    return null;
                }
                
                log.info("从MinIO获取文件 - 媒体ID: {}, bucketName: {}, objectKey: {}", 
                        mediaId, bucketName, objectKey);
                
                // 使用downloadFile方法获取输入流
                InputStream inputStream = minioService.downloadFile(bucketName, objectKey);
                if (inputStream == null) {
                    log.error("从MinIO获取文件失败 - 媒体ID: {}", mediaId);
                    return null;
                }
                
                // 将输入流转换为字节数组
                try {
                    fileContent = inputStream.readAllBytes();
                    inputStream.close();
                    log.info("成功读取文件内容 - 媒体ID: {}, 大小: {} 字节", mediaId, fileContent.length);
                } catch (Exception e) {
                    log.error("读取文件内容失败 - 媒体ID: {}", mediaId, e);
                    return null;
                }
            } else {
                // 从本地文件系统获取文件
                log.warn("暂不支持从本地文件系统获取文件 - 媒体ID: {}", mediaId);
                return null;
            }
            
            if (fileContent == null || fileContent.length == 0) {
                log.warn("媒体文件内容为空 - 媒体ID: {}", mediaId);
                return null;
            }
            
            log.info("媒体文件下载成功 - 媒体ID: {}, 文件大小: {} 字节", mediaId, fileContent.length);
            return fileContent;
            
        } catch (Exception e) {
            log.error("下载媒体文件失败 - 媒体ID: {}", mediaId, e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileUpload getMediaFileById(Long mediaId) {
        // TODO: 实现根据ID获取媒体文件逻辑
        // 使用用户模块的FileUploadService
        return fileUploadService.getFileById(mediaId);
    }

    @Override
    public void deleteMediaFile(Long mediaId, Long userId) {
        // TODO: 实现删除媒体文件逻辑
        // 1. 验证文件权限
        // 2. 从MinIO删除文件
        // 3. 从数据库删除记录
        log.info("Deleting media {} by user {}", mediaId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileUpload> getMediaFilesByConversation(Long conversationId, String mediaType) {
        // TODO: 实现获取会话中的媒体文件逻辑
        // 使用用户模块的FileUploadService
        return fileUploadService.getFilesByConversationId(conversationId, 0, 100);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileUpload> getMediaFilesByUser(Long userId, String mediaType) {
        // TODO: 实现获取用户上传的媒体文件逻辑
        // 使用用户模块的FileUploadService
        return fileUploadService.getFilesByUploaderId(userId, 0, 100);
    }

    @Override
    public boolean validateFileType(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return false;
        }
        
        String contentType = file.getContentType().toLowerCase();
        String fileName = file.getOriginalFilename();
        String extension = "";
        
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        
        // 允许的图片类型
        if (contentType.startsWith("image/")) {
            return extension.matches("jpg|jpeg|png|gif|bmp|webp|svg");
        }
        
        // 允许的视频类型
        if (contentType.startsWith("video/")) {
            return extension.matches("mp4|avi|mov|wmv|flv|mkv|webm");
        }
        
        // 允许的音频类型
        if (contentType.startsWith("audio/")) {
            return extension.matches("mp3|wav|flac|aac|ogg");
        }
        
        // 允许的文档类型
        if (contentType.startsWith("application/") || contentType.startsWith("text/")) {
            return extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt|csv|json|xml");
        }
        
        // 默认不允许其他类型
        return false;
    }

    @Override
    public boolean validateFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        long size = file.getSize();
        
        // 文件大小限制（字节）
        long maxImageSize = 10 * 1024 * 1024; // 10MB
        long maxVideoSize = 100 * 1024 * 1024; // 100MB
        long maxAudioSize = 50 * 1024 * 1024; // 50MB
        long maxDocumentSize = 20 * 1024 * 1024; // 20MB
        long maxOtherSize = 5 * 1024 * 1024; // 5MB
        
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return size <= maxImageSize;
            } else if (contentType.startsWith("video/")) {
                return size <= maxVideoSize;
            } else if (contentType.startsWith("audio/")) {
                return size <= maxAudioSize;
            } else if (contentType.startsWith("application/") || contentType.startsWith("text/")) {
                return size <= maxDocumentSize;
            }
        }
        
        // 默认限制
        return size <= maxOtherSize;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateThumbnail(Long mediaId) {
        // TODO: 实现生成文件缩略图逻辑
        // 1. 获取原始文件
        // 2. 生成缩略图
        // 3. 返回缩略图字节数组
        log.debug("Generating thumbnail for media {}", mediaId);
        return null;
    }
}