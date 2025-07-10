package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.user.config.MinioConfig;
import com.im.imcommunicationsystem.user.config.FileUploadConfig;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.event.FileOperationEvent;
import com.im.imcommunicationsystem.user.exception.FileUploadException;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.FileUploadService;
import com.im.imcommunicationsystem.user.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 基于MinIO的文件上传服务实现类
 * 提供文件上传到MinIO对象存储的功能
 */
@Service("minioFileUploadService")
@Primary
@Slf4j
@RequiredArgsConstructor
public class MinioFileUploadServiceImpl implements FileUploadService {

    private final MinioService minioService;
    private final MinioConfig minioConfig;
    private final FileUploadConfig fileUploadConfig;
    private final FileUploadRepository fileUploadRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 支持的文件类型
    private static final Map<String, FileUpload.FileType> FILE_TYPE_MAP = new HashMap<>();
    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> VIDEO_TYPES = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm");
    private static final Set<String> AUDIO_TYPES = Set.of("mp3", "wav", "flac", "aac", "ogg", "wma");
    private static final Set<String> DOCUMENT_TYPES = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    static {
        IMAGE_TYPES.forEach(ext -> FILE_TYPE_MAP.put(ext, FileUpload.FileType.image));
        VIDEO_TYPES.forEach(ext -> FILE_TYPE_MAP.put(ext, FileUpload.FileType.video));
        AUDIO_TYPES.forEach(ext -> FILE_TYPE_MAP.put(ext, FileUpload.FileType.audio));
        DOCUMENT_TYPES.forEach(ext -> FILE_TYPE_MAP.put(ext, FileUpload.FileType.document));
    }

    /**
     * 上传文件到MinIO
     */
    @Transactional
    public FileUpload uploadFile(MultipartFile file, Long userId) {
        log.info("开始上传文件到MinIO，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }

        try {
            // 基础验证
            validateFile(file);

            // 生成文件信息
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            FileUpload.FileType fileType = determineFileType(fileExtension);
            String bucketName = minioConfig.getBucketName(fileType.name(), true); // true表示私有文件
            String objectKey = generateObjectKey(originalFilename, fileType);
            String md5Hash = calculateMD5(file.getBytes());

            // 允许重复上传相同文件，通过唯一的objectKey进行区分
            log.debug("允许文件上传，文件MD5: {}, 文件名: {}", md5Hash, originalFilename);

            // 上传到MinIO
            boolean uploadSuccess = minioService.uploadFile(bucketName, objectKey, file);
            if (!uploadSuccess) {
                throw new FileUploadException("文件上传到MinIO失败");
            }

            // 创建文件记录
            FileUpload fileUpload = createFileUploadRecord(
                    file, userId, originalFilename, objectKey, bucketName, fileType, md5Hash
            );

            // 如果是图片，处理图片特殊信息
            if (fileType == FileUpload.FileType.image) {
                processImageInfo(file, fileUpload);
            }

            // 保存到数据库
            fileUpload = fileUploadRepository.save(fileUpload);
            log.info("文件上传成功，文件ID: {}, MinIO路径: {}/{}", fileUpload.getId(), bucketName, objectKey);

            // 发布文件上传事件
            try {
                FileOperationEvent uploadEvent = new FileOperationEvent(
                    this, 
                    fileUpload.getId().toString(), 
                    userId, 
                    FileOperationEvent.OperationType.UPLOAD, 
                    objectKey,
                    fileUpload.getOriginalName()
                );
                eventPublisher.publishEvent(uploadEvent);
                log.debug("已发布文件上传事件: fileId={}", fileUpload.getId());
            } catch (Exception e) {
                log.warn("发布文件上传事件失败: fileId={}", fileUpload.getId(), e);
            }

            return fileUpload;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传图片并生成缩略图
     */
    @Transactional
    public FileUpload uploadImage(MultipartFile file, Long userId, int maxWidth, int maxHeight) {
        log.info("开始上传图片到MinIO，用户ID: {}, 最大尺寸: {}x{}", userId, maxWidth, maxHeight);

        if (file.isEmpty()) {
            throw new FileUploadException("图片文件不能为空");
        }

        // 验证是否为图片文件
        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (!IMAGE_TYPES.contains(fileExtension.toLowerCase())) {
            throw new FileUploadException("不支持的图片格式，仅支持: " + IMAGE_TYPES);
        }

        try {
            // 基础验证
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String bucketName = minioConfig.getBucketName(FileUpload.FileType.image.name(), true); // true表示私有文件
            String objectKey = generateObjectKey(originalFilename, FileUpload.FileType.image);
            String md5Hash = calculateMD5(file.getBytes());

            // 允许重复上传相同图片，通过唯一的objectKey进行区分
            log.debug("允许图片上传，文件MD5: {}, 文件名: {}", md5Hash, originalFilename);

            // 处理图片压缩
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new FileUploadException("无法读取图片文件");
            }

            // 压缩图片
            BufferedImage resizedImage = resizeImage(originalImage, maxWidth, maxHeight);
            byte[] compressedImageBytes = imageToBytes(resizedImage, fileExtension);

            // 上传压缩后的图片
            ByteArrayInputStream compressedStream = new ByteArrayInputStream(compressedImageBytes);
            boolean uploadSuccess = minioService.uploadFile(
                    bucketName, objectKey, compressedStream, file.getContentType(), compressedImageBytes.length
            );

            if (!uploadSuccess) {
                throw new FileUploadException("图片上传到MinIO失败");
            }

            // 创建文件记录
            FileUpload fileUpload = createFileUploadRecord(
                    file, userId, originalFilename, objectKey, bucketName, FileUpload.FileType.image, md5Hash
            );

            // 设置图片信息
            fileUpload.setWidth(resizedImage.getWidth());
            fileUpload.setHeight(resizedImage.getHeight());
            fileUpload.setFileSize((long) compressedImageBytes.length);

            // 生成缩略图
            generateThumbnail(resizedImage, fileUpload, fileExtension);

            // 保存到数据库
            fileUpload = fileUploadRepository.save(fileUpload);
            log.info("图片上传成功，文件ID: {}, MinIO路径: {}/{}", fileUpload.getId(), bucketName, objectKey);

            // 发布文件上传事件
            try {
                FileOperationEvent uploadEvent = new FileOperationEvent(
                    this, 
                    fileUpload.getId().toString(), 
                    userId, 
                    FileOperationEvent.OperationType.UPLOAD, 
                    objectKey,
                    fileUpload.getOriginalName()
                );
                eventPublisher.publishEvent(uploadEvent);
                log.debug("已发布图片上传事件: fileId={}", fileUpload.getId());
            } catch (Exception e) {
                log.warn("发布图片上传事件失败: fileId={}", fileUpload.getId(), e);
            }

            return fileUpload;

        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new FileUploadException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @Transactional
    public boolean deleteFile(String fileId, Long userId) {
        log.info("删除文件，文件ID: {}, 用户ID: {}", fileId, userId);
        try {
            Long id = Long.parseLong(fileId);
            Optional<FileUpload> fileOptional = fileUploadRepository.findByIdAndIsDeletedFalse(id);
            if (fileOptional.isEmpty()) {
                log.warn("文件不存在或已删除: {}", fileId);
                return false;
            }

            FileUpload fileUpload = fileOptional.get();
            
            // 验证用户权限（只能删除自己的文件）
            if (!fileUpload.getUserId().equals(userId)) {
                throw new FileUploadException("无权限删除此文件");
            }
            
            // 先软删除数据库记录，确保数据一致性
            fileUpload.setIsDeleted(true);
            fileUpload.setDeletedAt(LocalDateTime.now());
            fileUploadRepository.save(fileUpload);
            
            try {
                // 从MinIO删除文件
                boolean mainFileDeleted = minioService.deleteFile(fileUpload.getBucketName(), fileUpload.getObjectKey());
                if (!mainFileDeleted) {
                    log.warn("MinIO主文件删除失败，但数据库已标记删除: {}", fileId);
                }
                
                // 删除缩略图（如果存在）
                if (fileUpload.getThumbnailUrl() != null) {
                    String thumbnailObjectKey = extractObjectKeyFromUrl(fileUpload.getThumbnailUrl());
                    boolean thumbnailDeleted = minioService.deleteFile(fileUpload.getBucketName(), thumbnailObjectKey);
                    if (!thumbnailDeleted) {
                        log.warn("MinIO缩略图删除失败: {}", thumbnailObjectKey);
                    }
                }
                
            } catch (Exception minioException) {
                log.error("MinIO删除操作失败，但数据库已标记删除: {}", fileId, minioException);
                // 不回滚数据库操作，因为软删除已完成，MinIO删除失败不影响业务逻辑
            }

            log.info("文件删除成功: {}", fileId);
            
            // 发布文件软删除事件
            try {
                FileOperationEvent deleteEvent = new FileOperationEvent(
                    this, 
                    fileId, 
                    userId, 
                    FileOperationEvent.OperationType.DELETE, 
                    fileUpload.getObjectKey(),
                    fileUpload.getOriginalName()
                );
                eventPublisher.publishEvent(deleteEvent);
                log.debug("已发布文件软删除事件: fileId={}", fileId);
            } catch (Exception e) {
                log.warn("发布文件软删除事件失败: fileId={}", fileId, e);
            }
            
            return true;

        } catch (Exception e) {
            log.error("文件删除失败: {}", fileId, e);
            throw new FileUploadException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    public Optional<FileUpload> getFileInfo(String fileId) {
        Long id = Long.parseLong(fileId);
        return fileUploadRepository.findByIdAndIsDeletedFalse(id);
    }

    /**
     * 物理删除文件（彻底删除）
     * 同时删除数据库记录和MinIO中的文件
     */
    @Transactional
    public boolean physicalDeleteFile(String fileId, Long userId) {
        log.info("物理删除文件，文件ID: {}, 用户ID: {}", fileId, userId);
        try {
            Long id = Long.parseLong(fileId);
            Optional<FileUpload> fileOptional = fileUploadRepository.findById(id);
            if (fileOptional.isEmpty()) {
                log.warn("文件不存在: {}", fileId);
                return false;
            }

            FileUpload fileUpload = fileOptional.get();
            
            // 验证用户权限（只能删除自己的文件）
            if (!fileUpload.getUserId().equals(userId)) {
                throw new FileUploadException("无权限删除此文件");
            }
            
            // 从MinIO删除文件
            boolean mainFileDeleted = minioService.deleteFile(fileUpload.getBucketName(), fileUpload.getObjectKey());
            if (!mainFileDeleted) {
                log.warn("MinIO主文件删除失败: {}", fileId);
            }
            
            // 删除缩略图（如果存在）
            if (fileUpload.getThumbnailUrl() != null) {
                String thumbnailObjectKey = extractObjectKeyFromUrl(fileUpload.getThumbnailUrl());
                boolean thumbnailDeleted = minioService.deleteFile(fileUpload.getBucketName(), thumbnailObjectKey);
                if (!thumbnailDeleted) {
                    log.warn("MinIO缩略图删除失败: {}", thumbnailObjectKey);
                }
            }
            
            // 物理删除数据库记录
            fileUploadRepository.deleteById(id);

            log.info("文件物理删除成功: {}", fileId);
            
            // 发布文件物理删除事件
            try {
                FileOperationEvent physicalDeleteEvent = new FileOperationEvent(
                    this, 
                    fileId, 
                    userId, 
                    FileOperationEvent.OperationType.PHYSICAL_DELETE, 
                    fileUpload.getObjectKey(),
                    fileUpload.getOriginalName()
                );
                eventPublisher.publishEvent(physicalDeleteEvent);
                log.debug("已发布文件物理删除事件: fileId={}", fileId);
            } catch (Exception e) {
                log.warn("发布文件物理删除事件失败: fileId={}", fileId, e);
            }
            
            return true;

        } catch (Exception e) {
            log.error("文件物理删除失败: {}", fileId, e);
            throw new FileUploadException("文件物理删除失败: " + e.getMessage());
        }
    }

    /**
     * 恢复已删除的文件
     */
    @Transactional
    public boolean restoreFile(String fileId, Long userId) {
        log.info("恢复文件，文件ID: {}, 用户ID: {}", fileId, userId);
        try {
            Long id = Long.parseLong(fileId);
            Optional<FileUpload> fileOptional = fileUploadRepository.findById(id);
            if (fileOptional.isEmpty()) {
                log.warn("文件不存在: {}", fileId);
                return false;
            }

            FileUpload fileUpload = fileOptional.get();
            
            // 验证用户权限（只能恢复自己的文件）
            if (!fileUpload.getUserId().equals(userId)) {
                throw new FileUploadException("无权限恢复此文件");
            }
            
            // 检查文件是否已删除
            if (!fileUpload.getIsDeleted()) {
                log.warn("文件未被删除，无需恢复: {}", fileId);
                return false;
            }
            
            // 检查MinIO中文件是否存在
            boolean minioFileExists = minioService.fileExists(fileUpload.getBucketName(), fileUpload.getObjectKey());
            if (!minioFileExists) {
                log.warn("MinIO中文件不存在，无法恢复: {}", fileId);
                throw new FileUploadException("文件在存储中不存在，无法恢复");
            }
            
            // 恢复数据库记录
            int updatedRows = fileUploadRepository.restoreFileByIdAndUserId(id, userId);
            if (updatedRows == 0) {
                log.warn("文件恢复失败，可能已被其他操作修改: {}", fileId);
                return false;
            }

            log.info("文件恢复成功: {}", fileId);
            
            // 发布文件恢复事件
            try {
                FileOperationEvent restoreEvent = new FileOperationEvent(
                    this, 
                    fileId, 
                    userId, 
                    FileOperationEvent.OperationType.RESTORE, 
                    fileUpload.getObjectKey(),
                    fileUpload.getOriginalName()
                );
                eventPublisher.publishEvent(restoreEvent);
                log.debug("已发布文件恢复事件: fileId={}", fileId);
            } catch (Exception e) {
                log.warn("发布文件恢复事件失败: fileId={}", fileId, e);
            }
            
            return true;

        } catch (Exception e) {
            log.error("文件恢复失败: {}", fileId, e);
            throw new FileUploadException("文件恢复失败: " + e.getMessage());
        }
    }

    /**
     * 批量物理删除过期的已删除文件
     * 清理指定时间之前软删除的文件
     */
    @Transactional
    public int cleanupExpiredFiles(int daysBeforeNow) {
        log.info("开始清理{}天前的已删除文件", daysBeforeNow);
        
        LocalDateTime expireTime = LocalDateTime.now().minusDays(daysBeforeNow);
        
        // 查询需要物理删除的文件
        List<FileUpload> expiredFiles = fileUploadRepository.findAll().stream()
                .filter(f -> f.getIsDeleted() && f.getDeletedAt() != null && f.getDeletedAt().isBefore(expireTime))
                .collect(java.util.stream.Collectors.toList());
        
        int deletedCount = 0;
        for (FileUpload fileUpload : expiredFiles) {
            try {
                // 从MinIO删除文件
                minioService.deleteFile(fileUpload.getBucketName(), fileUpload.getObjectKey());
                
                // 删除缩略图（如果存在）
                if (fileUpload.getThumbnailUrl() != null) {
                    String thumbnailObjectKey = extractObjectKeyFromUrl(fileUpload.getThumbnailUrl());
                    minioService.deleteFile(fileUpload.getBucketName(), thumbnailObjectKey);
                }
                
                deletedCount++;
            } catch (Exception e) {
                log.error("清理文件时MinIO删除失败: {}", fileUpload.getId(), e);
            }
        }
        
        // 批量物理删除数据库记录
        int dbDeletedCount = fileUploadRepository.physicalDeleteExpiredFiles(expireTime);
        
        log.info("清理完成，MinIO删除: {}个文件，数据库删除: {}条记录", deletedCount, dbDeletedCount);
        return dbDeletedCount;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        // 验证文件大小（50MB限制）
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new FileUploadException("文件大小不能超过50MB");
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileUploadException("文件名不能为空");
        }
    }

    /**
     * 获取文件扩展名
     */
    @Override
    public String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 确定文件类型
     */
    private FileUpload.FileType determineFileType(String extension) {
        return FILE_TYPE_MAP.getOrDefault(extension.toLowerCase(), FileUpload.FileType.other);
    }

    /**
     * 生成对象存储键
     */
    private String generateObjectKey(String originalFilename, FileUpload.FileType fileType) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFilename);
        
        return String.format("%s/%s/%s_%s.%s", 
                fileType.name().toLowerCase(), datePath, uuid, timestamp, extension);
    }

    /**
     * 计算文件MD5
     */
    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new FileUploadException("计算文件MD5失败");
        }
    }

    /**
     * 创建文件上传记录
     */
    private FileUpload createFileUploadRecord(
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
        fileUpload.setAccessLevel(FileUpload.AccessLevel.PRIVATE); // 设置为私有访问
        fileUpload.setIsPublic(false); // 兼容性字段
        fileUpload.setIsDeleted(false);
        fileUpload.setCreatedAt(LocalDateTime.now());
        fileUpload.setUpdatedAt(LocalDateTime.now());
        
        return fileUpload;
    }

    /**
     * 处理图片信息
     */
    private void processImageInfo(MultipartFile file, FileUpload fileUpload) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                fileUpload.setWidth(image.getWidth());
                fileUpload.setHeight(image.getHeight());
            }
        } catch (Exception e) {
            log.warn("获取图片尺寸失败: {}", fileUpload.getOriginalName(), e);
        }
    }

    /**
     * 压缩图片
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 计算缩放比例，确保图片不超过最大尺寸
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        // 如果比例大于等于1，说明原图已经小于等于目标尺寸，但仍需要进行质量压缩
        // 所以我们仍然创建新的BufferedImage以确保后续的质量压缩能够生效
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 保持原图的颜色模型和透明度
        int imageType = originalImage.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        }
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D g2d = resizedImage.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }

    /**
     * 图片转字节数组（支持压缩质量控制）
     */
    private byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = format.toLowerCase();
        
        if ("jpg".equals(formatName) || "jpeg".equals(formatName)) {
            // JPEG格式支持质量压缩
            formatName = "jpg";
            
            // 获取JPEG写入器
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                
                // 设置压缩质量
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(fileUploadConfig.getImageCompression().getQuality());
                
                // 写入图片
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
                
                ios.close();
                writer.dispose();
            } else {
                // 如果没有JPEG写入器，使用默认方式
                ImageIO.write(image, formatName, baos);
            }
        } else {
            // 其他格式使用默认方式
            ImageIO.write(image, formatName, baos);
        }
        
        return baos.toByteArray();
    }

    /**
     * 生成缩略图
     */
    private void generateThumbnail(BufferedImage originalImage, FileUpload fileUpload, String extension) {
        try {
            // 生成200x200的缩略图
            BufferedImage thumbnail = resizeImage(originalImage, 200, 200);
            byte[] thumbnailBytes = imageToBytes(thumbnail, extension);
            
            // 生成缩略图对象键
            String thumbnailObjectKey = fileUpload.getObjectKey().replace(
                    "." + extension, "_thumb." + extension
            );
            
            // 上传缩略图
            ByteArrayInputStream thumbnailStream = new ByteArrayInputStream(thumbnailBytes);
            boolean uploadSuccess = minioService.uploadFile(
                    fileUpload.getBucketName(), thumbnailObjectKey, 
                    thumbnailStream, fileUpload.getContentType(), thumbnailBytes.length
            );
            
            if (uploadSuccess) {
                String thumbnailUrl = minioConfig.getFileUrl(fileUpload.getBucketName(), thumbnailObjectKey);
                fileUpload.setThumbnailUrl(thumbnailUrl);
                log.info("缩略图生成成功: {}", thumbnailUrl);
            }
            
        } catch (Exception e) {
            log.warn("生成缩略图失败: {}", fileUpload.getOriginalName(), e);
        }
    }

    /**
     * 从对象键提取文件名
     */
    private String extractFilenameFromObjectKey(String objectKey) {
        int lastSlashIndex = objectKey.lastIndexOf('/');
        return lastSlashIndex == -1 ? objectKey : objectKey.substring(lastSlashIndex + 1);
    }

    /**
     * 从URL提取对象键
     */
    private String extractObjectKeyFromUrl(String url) {
        // 简单实现，实际可能需要更复杂的URL解析
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return url;
    }

    /**
     * 获取用户文件列表（分页）
     */
    @Override
    public List<FileUpload> getUserFiles(Long userId, FileUpload.FileType fileType, int page, int size) {
        log.info("开始查询用户文件 - 用户ID: {}, 文件类型: {}, 页码: {}, 大小: {}", userId, fileType, page, size);
        try {
            // 先检查数据库中是否有任何文件记录
            long totalCount = fileUploadRepository.count();
            log.info("数据库中总文件记录数: {}", totalCount);
            
            // 检查该用户是否有任何文件记录（包括已删除的）
            List<FileUpload> allUserFiles = fileUploadRepository.findAll().stream()
                .filter(f -> f.getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
            log.info("用户 {} 的所有文件记录数（包括已删除）: {}", userId, allUserFiles.size());
            
            if (!allUserFiles.isEmpty()) {
                for (FileUpload file : allUserFiles) {
                    log.info("文件记录: ID={}, 原始名称={}, 是否删除={}, 文件类型={}", 
                        file.getId(), file.getOriginalName(), file.getIsDeleted(), file.getFileType());
                }
            }
            
            Pageable pageable = PageRequest.of(page, size);
            if (fileType != null) {
                log.info("按文件类型查询: {}", fileType);
                Page<FileUpload> filePage = fileUploadRepository.findByUserIdAndFileTypeAndIsDeletedFalse(userId, fileType, pageable);
                log.info("数据库查询结果 - 总数: {}, 当前页数量: {}", filePage.getTotalElements(), filePage.getContent().size());
                return filePage.getContent();
            } else {
                log.info("查询所有文件类型");
                Page<FileUpload> filePage = fileUploadRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
                log.info("数据库查询结果 - 总数: {}, 当前页数量: {}", filePage.getTotalElements(), filePage.getContent().size());
                return filePage.getContent();
            }
        } catch (Exception e) {
            log.error("获取用户文件列表失败, userId: {}, fileType: {}", userId, fileType, e);
            throw new RuntimeException("获取文件列表失败", e);
        }
    }

    /**
     * 获取用户文件统计信息
     */
    @Override
    public Map<String, Object> getUserFileStats(Long userId) {
        try {
            log.info("开始获取用户文件统计信息, userId: {}", userId);
            
            // 获取用户所有文件
            List<FileUpload> userFiles = fileUploadRepository.findByUserIdAndIsDeletedFalse(userId);
            log.info("查询到用户文件数量: {}", userFiles.size());
            
            // 打印每个文件的详细信息
            for (FileUpload file : userFiles) {
                log.info("文件详情: ID={}, 名称={}, 类型={}, 大小={}", 
                    file.getId(), file.getOriginalName(), file.getFileType(), file.getFileSize());
            }
            
            // 计算统计信息
            long totalFiles = userFiles.size();
            long totalSize = userFiles.stream().mapToLong(FileUpload::getFileSize).sum();
            
            // 按文件类型统计
            Map<String, Long> typeStats = userFiles.stream()
                    .collect(Collectors.groupingBy(
                            file -> file.getFileType().name(),
                            Collectors.counting()
                    ));
            
            log.info("文件类型统计结果: {}", typeStats);
            
            // 按文件类型统计大小
            Map<String, Long> typeSizeStats = userFiles.stream()
                    .collect(Collectors.groupingBy(
                            file -> file.getFileType().name(),
                            Collectors.summingLong(FileUpload::getFileSize)
                    ));
            
            // 构建返回结果
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFiles", totalFiles);
            stats.put("totalSize", totalSize);
            stats.put("typeStats", typeStats);
            stats.put("typeSizeStats", typeSizeStats);
            
            log.info("最终统计结果: totalFiles={}, totalSize={}, typeStats={}", 
                totalFiles, totalSize, typeStats);
            
            return stats;
            
        } catch (Exception e) {
            log.error("获取用户文件统计信息失败, userId: {}", userId, e);
            throw new RuntimeException("获取文件统计信息失败", e);
        }
    }

    // ==================== FileUploadService接口方法实现 ====================

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        // 这个方法暂时不实现，因为MinIO版本使用不同的上传方法
        throw new UnsupportedOperationException("请使用 uploadFile(MultipartFile file, Long userId) 方法");
    }

    @Override
    public String uploadImage(MultipartFile file, String directory, int maxWidth, int maxHeight) {
        // 这个方法暂时不实现，因为MinIO版本使用不同的上传方法
        throw new UnsupportedOperationException("请使用 uploadImage(MultipartFile file, Long userId, int maxWidth, int maxHeight) 方法");
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        // 这个方法暂时不实现，因为MinIO版本使用不同的删除方法
        throw new UnsupportedOperationException("请使用 deleteFile(String fileId, Long userId) 方法");
    }

    @Override
    public boolean validateFileType(MultipartFile file, String[] allowedTypes) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        return Arrays.asList(allowedTypes).contains(extension);
    }

    @Override
    public String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        return uuid + "_" + timestamp + "." + extension;
    }
}