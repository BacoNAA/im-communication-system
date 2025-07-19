package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.user.config.MinioConfig;
import com.im.imcommunicationsystem.user.entity.FileUpload;

import com.im.imcommunicationsystem.user.exception.FileUploadException;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.MinioService;
import com.im.imcommunicationsystem.user.service.PublicFileUploadService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 公开文件上传服务实现类
 * 专门处理公开访问的文件（如头像、公开图片等）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicFileUploadServiceImpl implements PublicFileUploadService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final MinioService minioService;
    private final FileUploadRepository fileUploadRepository;
    
    @Value("${app.file.temporary-file-cleanup.default-expiration-days:7}")
    private int temporaryFileExpirationDays;


    @Override
    @Transactional
    public String uploadFile(MultipartFile file, Long userId) {
        validateFile(file);
        
        try {
            // 生成文件名和路径
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateFileName(fileExtension);
            
            // 确定文件类型和存储桶
            FileUpload.FileType fileType = FileUpload.FileType.fromContentType(file.getContentType());
            String bucketName = minioConfig.getBucketName(fileType.getValue(), false); // false表示公开文件
            String objectKey = generateObjectKey(userId, fileType, fileName);
            
            // 计算文件MD5
            String md5Hash = calculateMD5(file.getInputStream());
            
            // 准备文件标签
            Map<String, String> tags = new HashMap<>();
            tags.put("file_tag", "TEMPORARY");
            tags.put("user_id", String.valueOf(userId));
            tags.put("upload_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // 上传到MinIO并设置标签
            if (!uploadToMinioWithTags(bucketName, objectKey, file, tags)) {
                throw new RuntimeException("文件上传到MinIO失败");
            }
            
            // 创建文件记录（默认为临时文件，7天后过期）
            FileUpload fileUpload = createPublicFileUploadRecord(
                file, userId, originalFilename, objectKey, bucketName, fileType, md5Hash
            );
            
            // 设置为临时文件，配置天数后过期
            fileUpload.setFileTag(FileUpload.FileTag.TEMPORARY);
            fileUpload.setExpiresAt(LocalDateTime.now().plusDays(temporaryFileExpirationDays));
            
            // 保存到数据库
            fileUpload = fileUploadRepository.save(fileUpload);
            

            
            log.info("公开文件上传成功: userId={}, fileId={}, fileName={}", 
                    userId, fileUpload.getId(), originalFilename);
            
            return fileUpload.getFileUrl();
            
        } catch (Exception e) {
            log.error("公开文件上传失败: userId={}, fileName={}", userId, file.getOriginalFilename(), e);
            throw new FileUploadException("公开文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public String uploadImage(MultipartFile file, Long userId, int maxWidth, int maxHeight) {
        validateImageFile(file);
        
        try {
            // 压缩图片
            byte[] compressedImageData = compressImage(file, maxWidth, maxHeight);
            
            // 创建压缩后的文件对象
            MultipartFile compressedFile = createMultipartFile(
                compressedImageData, file.getOriginalFilename(), file.getContentType()
            );
            
            return uploadFile(compressedFile, userId);
            
        } catch (Exception e) {
            log.error("公开图片上传失败: userId={}, fileName={}", userId, file.getOriginalFilename(), e);
            throw new FileUploadException("公开图片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public String uploadAvatar(MultipartFile file, Long userId) {
        log.info("开始上传用户头像: userId={}, fileName={}", userId, file.getOriginalFilename());
        
        // 删除用户的旧头像（如果存在）
        deleteOldAvatar(userId);
        
        validateImageFile(file);
        
        try {
            // 压缩图片为 200x200
            byte[] compressedImageData = compressImage(file, 200, 200);
            
            // 创建压缩后的文件对象
            MultipartFile compressedFile = createMultipartFile(
                compressedImageData, file.getOriginalFilename(), file.getContentType()
            );
            
            // 生成文件名和路径
            String originalFilename = compressedFile.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateFileName(fileExtension);
            
            // 确定文件类型和存储桶
            FileUpload.FileType fileType = FileUpload.FileType.fromContentType(compressedFile.getContentType());
            String bucketName = minioConfig.getBucketName(fileType.getValue(), false); // false表示公开文件
            String objectKey = generateObjectKey(userId, fileType, fileName);
            
            // 计算文件MD5
            String md5Hash = calculateMD5(compressedFile.getInputStream());
            
            // 准备永久文件标签
            Map<String, String> tags = new HashMap<>();
            tags.put("file_tag", "PERMANENT");
            tags.put("user_id", String.valueOf(userId));
            tags.put("file_type", "avatar");
            tags.put("upload_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // 上传到MinIO并设置标签
            if (!uploadToMinioWithTags(bucketName, objectKey, compressedFile, tags)) {
                throw new RuntimeException("头像上传到MinIO失败");
            }
            
            // 创建文件记录（直接设置为永久文件）
            FileUpload fileUpload = createPublicFileUploadRecord(
                compressedFile, userId, originalFilename, objectKey, bucketName, fileType, md5Hash
            );
            
            // 设置为永久文件
            fileUpload.setFileTag(FileUpload.FileTag.PERMANENT);
            fileUpload.setExpiresAt(null); // 永久文件不设置过期时间
            
            // 保存到数据库
            fileUpload = fileUploadRepository.save(fileUpload);
            
            log.info("用户头像上传成功: userId={}, fileId={}, fileName={}", 
                    userId, fileUpload.getId(), originalFilename);
            
            return fileUpload.getFileUrl();
            
        } catch (Exception e) {
            log.error("用户头像上传失败: userId={}, fileName={}", userId, file.getOriginalFilename(), e);
            throw new FileUploadException("用户头像上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传群组头像
     * 与用户头像不同，旧的群组头像会被保留为私有且临时状态
     * 
     * @param file 群组头像文件
     * @param groupId 群组ID
     * @param userId 操作用户ID
     * @return 头像URL
     */
    @Override
    @Transactional
    public String uploadGroupAvatar(MultipartFile file, Long groupId, Long userId) {
        log.info("开始上传群组头像: groupId={}, userId={}, fileName={}", groupId, userId, file.getOriginalFilename());
        
        // 查找并更改旧的群组头像状态
        changeOldGroupAvatarStatus(groupId);
        
        validateImageFile(file);
        
        try {
            // 压缩图片为 200x200
            byte[] compressedImageData = compressImage(file, 200, 200);
            
            // 创建压缩后的文件对象
            MultipartFile compressedFile = createMultipartFile(
                compressedImageData, file.getOriginalFilename(), file.getContentType()
            );
            
            // 生成文件名和路径
            String originalFilename = compressedFile.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateFileName(fileExtension);
            
            // 确定文件类型和存储桶 - 使用 "group_avatar" 作为特殊类型
            FileUpload.FileType fileType = FileUpload.FileType.fromContentType(compressedFile.getContentType());
            String bucketName = minioConfig.getBucketName(fileType.getValue(), false); // false表示公开文件
            String objectKey = "group_avatars/" + groupId + "/" + fileName;
            
            // 计算文件MD5
            String md5Hash = calculateMD5(compressedFile.getInputStream());
            
            // 准备永久文件标签
            Map<String, String> tags = new HashMap<>();
            tags.put("file_tag", "PERMANENT");
            tags.put("user_id", String.valueOf(userId));
            tags.put("group_id", String.valueOf(groupId));
            tags.put("file_type", "group_avatar");
            tags.put("upload_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // 上传到MinIO并设置标签
            if (!uploadToMinioWithTags(bucketName, objectKey, compressedFile, tags)) {
                throw new RuntimeException("群组头像上传到MinIO失败");
            }
            
            // 创建文件记录（直接设置为永久文件）
            FileUpload fileUpload = createPublicFileUploadRecord(
                compressedFile, userId, originalFilename, objectKey, bucketName, fileType, md5Hash
            );
            
            // 设置为永久文件并添加群组ID
            fileUpload.setFileTag(FileUpload.FileTag.PERMANENT);
            fileUpload.setExpiresAt(null); // 永久文件不设置过期时间
            
            // 添加群组ID到元数据（JSON格式）
            try {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("groupId", String.valueOf(groupId));
                metadata.put("avatarType", "group");
                fileUpload.setMetadata(new ObjectMapper().writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("设置元数据失败: {}", e.getMessage());
            }
            
            // 保存到数据库
            fileUpload = fileUploadRepository.save(fileUpload);
            
            log.info("群组头像上传成功: groupId={}, userId={}, fileId={}, fileUrl={}", 
                    groupId, userId, fileUpload.getId(), fileUpload.getFileUrl());
            
            return fileUpload.getFileUrl();
            
        } catch (Exception e) {
            log.error("群组头像上传失败: groupId={}, userId={}, fileName={}", 
                    groupId, userId, file.getOriginalFilename(), e);
            throw new FileUploadException("群组头像上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteFile(Long fileId, Long userId) {
        try {
            FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new FileUploadException("文件不存在: " + fileId));
            
            // 检查文件所有权
            if (!fileUpload.getUserId().equals(userId)) {
                throw new FileUploadException("无权限删除此文件");
            }
            
            // 检查是否为公开文件
            if (!fileUpload.getAccessLevel().isPublic()) {
                throw new FileUploadException("此文件不是公开文件");
            }
            
            // 软删除
            fileUpload.setIsDeleted(true);
            fileUpload.setDeletedAt(LocalDateTime.now());
            fileUploadRepository.save(fileUpload);
            
            log.info("公开文件软删除成功: fileId={}, userId={}", fileId, userId);
            return true;
            
        } catch (Exception e) {
            log.error("公开文件软删除失败: fileId={}, userId={}", fileId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean physicalDeleteFile(Long fileId, Long userId) {
        try {
            FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new FileUploadException("文件不存在: " + fileId));
            
            // 检查文件所有权
            if (!fileUpload.getUserId().equals(userId)) {
                throw new FileUploadException("无权限删除此文件");
            }
            
            // 检查是否为公开文件
            if (!fileUpload.getAccessLevel().isPublic()) {
                throw new FileUploadException("此文件不是公开文件");
            }
            
            // 从MinIO删除文件
            deleteFromMinio(fileUpload.getBucketName(), fileUpload.getObjectKey());
            
            // 从数据库删除记录
            fileUploadRepository.delete(fileUpload);
            
            log.info("公开文件物理删除成功: fileId={}, userId={}", fileId, userId);
            return true;
            
        } catch (Exception e) {
            log.error("公开文件物理删除失败: fileId={}, userId={}", fileId, userId, e);
            return false;
        }
    }

    @Override
    public List<FileUpload> getUserPublicFiles(Long userId, FileUpload.FileType fileType) {
        if (fileType != null) {
            return fileUploadRepository.findByUserIdAndFileTypeAndAccessLevelAndIsDeletedFalse(
                userId, fileType, FileUpload.AccessLevel.PUBLIC
            );
        } else {
            return fileUploadRepository.findByUserIdAndAccessLevelAndIsDeletedFalse(
                userId, FileUpload.AccessLevel.PUBLIC
            );
        }
    }

    @Override
    public FileUpload getPublicFileById(Long fileId) {
        return fileUploadRepository.findByIdAndAccessLevelAndIsDeletedFalse(
            fileId, FileUpload.AccessLevel.PUBLIC
        ).orElse(null);
    }

    @Override
    public boolean isPublicFile(Long fileId) {
        return fileUploadRepository.existsByIdAndAccessLevelAndIsDeletedFalse(
            fileId, FileUpload.AccessLevel.PUBLIC
        );
    }

    @Override
    public String getPublicFileUrl(Long fileId) {
        FileUpload fileUpload = getPublicFileById(fileId);
        return fileUpload != null ? fileUpload.getFileUrl() : null;
    }

    // 私有辅助方法
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB限制
            throw new FileUploadException("文件大小不能超过50MB");
        }
    }
    
    private void validateImageFile(MultipartFile file) {
        validateFile(file);
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileUploadException("只支持图片文件");
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    private String generateFileName(String extension) {
        return UUID.randomUUID().toString() + extension;
    }
    
    private String generateObjectKey(Long userId, FileUpload.FileType fileType, String fileName) {
        return String.format("public/%s/%s/%s", 
            fileType.getValue(), userId, fileName);
    }
    
    private String calculateMD5(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    private void uploadToMinio(MultipartFile file, String bucketName, String objectKey) throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
    }
    
    /**
     * 上传文件到MinIO并设置标签
     */
    private boolean uploadToMinioWithTags(String bucketName, String objectKey, MultipartFile file, Map<String, String> tags) {
        return minioService.uploadFileWithTags(bucketName, objectKey, file, tags);
    }
    
    private void deleteFromMinio(String bucketName, String objectKey) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build()
        );
    }
    
    private FileUpload createPublicFileUploadRecord(
            MultipartFile file, Long userId, String originalFilename, 
            String objectKey, String bucketName, FileUpload.FileType fileType, String md5Hash) {
        
        FileUpload fileUpload = new FileUpload();
        fileUpload.setUserId(userId);
        fileUpload.setOriginalName(originalFilename);
        fileUpload.setFileName(extractFilenameFromObjectKey(objectKey));
        fileUpload.setFilePath(objectKey);
        fileUpload.setFileUrl(minioConfig.getFileUrl(bucketName, objectKey));
        fileUpload.setFileSize(file.getSize());
        fileUpload.setContentType(file.getContentType());
        fileUpload.setFileType(fileType);
        fileUpload.setMd5Hash(md5Hash);
        fileUpload.setStorageType(FileUpload.StorageType.minio);
        fileUpload.setBucketName(bucketName);
        fileUpload.setObjectKey(objectKey);
        fileUpload.setAccessLevel(FileUpload.AccessLevel.PUBLIC); // 设置为公开访问
        fileUpload.setIsPublic(true); // 兼容性字段
        fileUpload.setIsDeleted(false);
        
        return fileUpload;
    }
    
    private String extractFilenameFromObjectKey(String objectKey) {
        return objectKey.substring(objectKey.lastIndexOf("/") + 1);
    }
    

    
    private byte[] compressImage(MultipartFile file, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        if (originalImage == null) {
            throw new FileUploadException("无法读取图片文件");
        }
        
        // 计算压缩后的尺寸
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 创建压缩后的图片
        BufferedImage compressedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = compressedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = getImageFormat(file.getContentType());
        ImageIO.write(compressedImage, formatName, baos);
        
        return baos.toByteArray();
    }
    
    private String getImageFormat(String contentType) {
        if (contentType.contains("png")) {
            return "png";
        } else if (contentType.contains("gif")) {
            return "gif";
        } else {
            return "jpg";
        }
    }
    
    private MultipartFile createMultipartFile(byte[] data, String originalFilename, String contentType) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }
            
            @Override
            public String getOriginalFilename() {
                return originalFilename;
            }
            
            @Override
            public String getContentType() {
                return contentType;
            }
            
            @Override
            public boolean isEmpty() {
                return data.length == 0;
            }
            
            @Override
            public long getSize() {
                return data.length;
            }
            
            @Override
            public byte[] getBytes() {
                return data;
            }
            
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(data);
            }
            
            @Override
            public void transferTo(java.io.File dest) throws IOException {
                throw new UnsupportedOperationException("transferTo not supported");
            }
        };
    }
    
    /**
     * 删除用户的旧头像
     */
    private void deleteOldAvatar(Long userId) {
        try {
            // 查找用户现有的头像文件（永久标签的图片文件）
            List<FileUpload> oldAvatars = fileUploadRepository.findByUserIdAndFileTypeAndFileTagAndIsDeletedFalse(
                userId, FileUpload.FileType.image, FileUpload.FileTag.PERMANENT
            ).stream()
            .filter(file -> file.getAccessLevel() == FileUpload.AccessLevel.PUBLIC)
            .collect(java.util.stream.Collectors.toList());
            
            for (FileUpload oldAvatar : oldAvatars) {
                // 物理删除旧头像
                physicalDeleteFile(oldAvatar.getId(), userId);
                log.info("删除用户旧头像: userId={}, fileId={}", userId, oldAvatar.getId());
            }
            
        } catch (Exception e) {
            log.warn("删除用户旧头像失败: userId={}", userId, e);
        }
    }
    
    /**
     * 更改旧的群组头像状态为私有和临时
     * 
     * @param groupId 群组ID
     */
    private void changeOldGroupAvatarStatus(Long groupId) {
        try {
            // 查找群组现有的头像文件
            List<FileUpload> fileUploads = fileUploadRepository.findAll();
            List<FileUpload> oldGroupAvatars = fileUploads.stream()
                .filter(file -> {
                    if (file.getMetadata() == null) return false;
                    try {
                        Map<String, String> metadata = new ObjectMapper().readValue(file.getMetadata(), Map.class);
                        String fileGroupId = metadata.get("groupId");
                        return fileGroupId != null && fileGroupId.equals(String.valueOf(groupId)) &&
                               "group".equals(metadata.get("avatarType")) &&
                               file.getFileTag() == FileUpload.FileTag.PERMANENT &&
                               !file.getIsDeleted();
                    } catch (Exception e) {
                        log.warn("解析元数据失败: {}", e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            for (FileUpload oldAvatar : oldGroupAvatars) {
                log.info("更改旧群组头像状态: groupId={}, fileId={}, fileUrl={}",
                        groupId, oldAvatar.getId(), oldAvatar.getFileUrl());
                
                // 设置为私有和临时
                oldAvatar.setAccessLevel(FileUpload.AccessLevel.PRIVATE);
                oldAvatar.setIsPublic(false);
                oldAvatar.setFileTag(FileUpload.FileTag.TEMPORARY);
                oldAvatar.setExpiresAt(LocalDateTime.now().plusDays(temporaryFileExpirationDays));
                fileUploadRepository.save(oldAvatar);
                
                // 同时更新MinIO中的文件标签
                Map<String, String> tags = new HashMap<>();
                tags.put("file_tag", "TEMPORARY");
                tags.put("group_id", String.valueOf(groupId));
                tags.put("is_deprecated", "true");
                tags.put("updated_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                
                minioService.setObjectTags(oldAvatar.getBucketName(), oldAvatar.getObjectKey(), tags);
            }
            
            log.info("更改旧群组头像状态完成: groupId={}, 处理 {} 个文件", groupId, oldGroupAvatars.size());
            
        } catch (Exception e) {
            log.warn("更改旧群组头像状态失败: groupId={}", groupId, e);
        }
    }
    
    /**
     * 将文件标记为永久文件
     */
    private void markFileAsPermanent(String fileUrl, Long userId) {
        try {
            // 根据文件URL查找文件记录
            List<FileUpload> userFiles = fileUploadRepository.findByUserIdAndIsDeletedFalse(userId);
            FileUpload fileUpload = userFiles.stream()
                    .filter(file -> fileUrl.equals(file.getFileUrl()))
                    .findFirst()
                    .orElse(null);
            
            if (fileUpload != null) {
                fileUpload.setFileTag(FileUpload.FileTag.PERMANENT);
                fileUpload.setExpiresAt(null); // 永久文件不设置过期时间
                fileUploadRepository.save(fileUpload);
                
                // 同时更新MinIO中的文件标签
                Map<String, String> tags = new HashMap<>();
                tags.put("file_tag", "PERMANENT");
                tags.put("user_id", String.valueOf(userId));
                tags.put("updated_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                
                String bucketName = fileUpload.getBucketName();
                String objectKey = fileUpload.getObjectKey();
                if (minioService.setObjectTags(bucketName, objectKey, tags)) {
                    log.info("文件已标记为永久（包含MinIO标签）: fileId={}, userId={}", fileUpload.getId(), userId);
                } else {
                    log.warn("文件数据库记录已更新为永久，但MinIO标签更新失败: fileId={}, userId={}", fileUpload.getId(), userId);
                }
            }
            
        } catch (Exception e) {
            log.warn("标记文件为永久失败: fileUrl={}, userId={}", fileUrl, userId, e);
        }
    }
}