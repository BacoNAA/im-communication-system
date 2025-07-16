package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.user.config.FileUploadConfig;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.exception.FileUploadException;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 文件上传服务实现类
 * 处理文件上传、图片压缩等功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileUploadConfig fileUploadConfig;
    private final FileUploadRepository fileUploadRepository;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        log.info("开始上传文件，目录: {}, 文件名: {}", directory, file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }
        
        // 验证文件大小
        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new FileUploadException("文件大小不能超过" + (fileUploadConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        try {
            // 创建目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fullDirectory = directory + "/" + datePath;
            Path directoryPath = Paths.get(fileUploadConfig.getUploadPath(), fullDirectory);
            Files.createDirectories(directoryPath);
            
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());
            Path filePath = directoryPath.resolve(fileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String fileUrl = fileUploadConfig.getBaseUrl() + "/uploads/" + fullDirectory + "/" + fileName;
            log.info("文件上传成功，URL: {}", fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String directory, int maxWidth, int maxHeight) {
        log.info("开始上传图片，目录: {}, 最大尺寸: {}x{}", directory, maxWidth, maxHeight);
        
        if (file.isEmpty()) {
            throw new FileUploadException("图片文件不能为空");
        }
        
        // 验证文件类型
        if (!isValidImageFile(file)) {
            throw new FileUploadException("不支持的图片格式，仅支持: " + Arrays.toString(fileUploadConfig.getAllowedImageTypes()));
        }
        
        // 验证文件大小
        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new FileUploadException("图片大小不能超过" + (fileUploadConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new FileUploadException("无法读取图片文件");
            }
            
            // 压缩图片
            BufferedImage resizedImage = resizeImage(originalImage, maxWidth, maxHeight);
            
            // 创建目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fullDirectory = directory + "/" + datePath;
            Path directoryPath = Paths.get(fileUploadConfig.getUploadPath(), fullDirectory);
            Files.createDirectories(directoryPath);
            
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());
            Path filePath = directoryPath.resolve(fileName);
            
            // 保存压缩后的图片
            String formatName = getFileExtension(fileName).toLowerCase();
            if ("jpg".equals(formatName) || "jpeg".equals(formatName)) {
                formatName = "jpg";
            }
            ImageIO.write(resizedImage, formatName, filePath.toFile());
            
            String fileUrl = fileUploadConfig.getBaseUrl() + "/uploads/" + fullDirectory + "/" + fileName;
            log.info("图片上传成功，URL: {}", fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new FileUploadException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        log.info("删除文件，URL: {}", fileUrl);
        
        try {
            // 从URL中提取文件路径
            String relativePath = fileUrl.replace(fileUploadConfig.getBaseUrl() + "/uploads/", "");
            Path filePath = Paths.get(fileUploadConfig.getUploadPath(), relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", fileUrl);
                return true;
            } else {
                log.warn("文件不存在: {}", fileUrl);
                return false;
            }
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileUrl, e);
            return false;
        }
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

    /**
     * 验证是否为有效的图片文件
     */
    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return Arrays.asList(fileUploadConfig.getAllowedImageTypes()).contains(contentType);
    }

    @Override
    public String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        return uuid + "_" + timestamp + "." + extension;
    }

    @Override
    public String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1);
    }

    @Override
    public List<FileUpload> getUserFiles(Long userId, FileUpload.FileType fileType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (fileType != null) {
            return fileUploadRepository.findByUserIdAndFileTypeAndIsDeletedFalse(userId, fileType, pageable).getContent();
        } else {
            return fileUploadRepository.findByUserIdAndIsDeletedFalse(userId, pageable).getContent();
        }
    }

    @Override
    public Map<String, Object> getUserFileStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总文件数
        Long totalFiles = fileUploadRepository.countByUserIdAndIsDeletedFalse(userId);
        stats.put("totalFiles", totalFiles);
        
        // 总文件大小
        Long totalSize = fileUploadRepository.sumFileSizeByUserIdAndIsDeletedFalse(userId);
        stats.put("totalSize", totalSize);
        
        // 按类型统计
        Map<String, Long> typeStats = new HashMap<>();
        for (FileUpload.FileType fileType : FileUpload.FileType.values()) {
            Long count = fileUploadRepository.countByUserIdAndFileTypeAndIsDeletedFalse(userId, fileType);
            typeStats.put(fileType.name(), count);
        }
        stats.put("typeStats", typeStats);
        
        return stats;
    }

    // ==================== 消息模块特有功能实现 ====================

    @Override
    @Transactional
    public FileUpload uploadMediaFile(MultipartFile file, Long uploaderId, Long conversationId, Long messageId) {
        log.info("开始上传媒体文件，上传者ID: {}, 会话ID: {}, 消息ID: {}", uploaderId, conversationId, messageId);
        
        if (file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }
        
        // 验证文件大小
        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new FileUploadException("文件大小不能超过" + (fileUploadConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        try {
            // 创建FileUpload实体
            FileUpload fileUpload = new FileUpload();
            fileUpload.setUserId(uploaderId);
            fileUpload.setConversationId(conversationId);
            fileUpload.setMessageId(messageId);
            fileUpload.setOriginalName(file.getOriginalFilename());
            fileUpload.setFileSize(file.getSize());
            fileUpload.setMimeType(file.getContentType());
            
            // 确定文件类型
            String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
            FileUpload.FileType fileType = determineFileType(file.getContentType(), extension);
            fileUpload.setFileType(fileType);
            
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());
            fileUpload.setFileName(fileName);
            
            // 创建目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fullDirectory = "media/" + datePath;
            Path directoryPath = Paths.get(fileUploadConfig.getUploadPath(), fullDirectory);
            Files.createDirectories(directoryPath);
            
            // 保存文件
            Path filePath = directoryPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 设置文件路径和URL
            String relativePath = fullDirectory + "/" + fileName;
            fileUpload.setFilePath(relativePath);
            String fileUrl = fileUploadConfig.getBaseUrl() + "/uploads/" + relativePath;
            fileUpload.setFileUrl(fileUrl);
            
            // 计算MD5哈希值
            // TODO: 实现MD5计算
            
            // 设置其他属性
            fileUpload.setStorageType(FileUpload.StorageType.LOCAL);
            fileUpload.setAccessLevel(FileUpload.AccessLevel.PRIVATE);
            fileUpload.setFileTag(FileUpload.FileTag.PERMANENT);
            fileUpload.setCreatedAt(LocalDateTime.now());
            fileUpload.setUpdatedAt(LocalDateTime.now());
            
            // 保存到数据库
            FileUpload savedFile = fileUploadRepository.save(fileUpload);
            
            log.info("媒体文件上传成功，文件ID: {}, URL: {}", savedFile.getId(), fileUrl);
            return savedFile;
            
        } catch (IOException e) {
            log.error("媒体文件上传失败", e);
            throw new FileUploadException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileUpload getFileById(Long fileId) {
        return fileUploadRepository.findByIdAndIsDeletedFalse(fileId).orElse(null);
    }

    @Override
    public List<FileUpload> getFilesByConversationId(Long conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fileUploadRepository.findByConversationIdAndIsDeletedFalse(conversationId, pageable).getContent();
    }

    @Override
    public FileUpload getFileByMessageId(Long messageId) {
        return fileUploadRepository.findByMessageIdAndIsDeletedFalse(messageId).orElse(null);
    }

    @Override
    public List<FileUpload> getFilesByUploaderId(Long uploaderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return fileUploadRepository.findMediaFilesByUploaderIdAndIsDeletedFalse(uploaderId, pageable).getContent();
    }

    @Override
    @Transactional
    public boolean softDeleteFile(Long fileId) {
        try {
            int result = fileUploadRepository.softDeleteById(fileId, LocalDateTime.now());
            return result > 0;
        } catch (Exception e) {
            log.error("软删除文件失败，文件ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updateFileAssociation(Long fileId, Long conversationId, Long messageId) {
        try {
            int result = fileUploadRepository.updateFileAssociation(fileId, conversationId, messageId);
            return result > 0;
        } catch (Exception e) {
            log.error("更新文件关联失败，文件ID: {}, 会话ID: {}, 消息ID: {}", fileId, conversationId, messageId, e);
            return false;
        }
    }

    /**
     * 根据MIME类型和文件扩展名确定文件类型
     */
    private FileUpload.FileType determineFileType(String mimeType, String extension) {
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return FileUpload.FileType.image;
            } else if (mimeType.startsWith("video/")) {
                return FileUpload.FileType.video;
            } else if (mimeType.startsWith("audio/")) {
                return FileUpload.FileType.audio;
            } else if (mimeType.startsWith("text/") || mimeType.contains("document") || mimeType.contains("pdf")) {
                return FileUpload.FileType.document;
            }
        }
        
        // 根据扩展名判断
        if (extension != null) {
            switch (extension) {
                case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp":
                    return FileUpload.FileType.image;
                case "mp4": case "avi": case "mov": case "wmv": case "flv": case "webm":
                    return FileUpload.FileType.video;
                case "mp3": case "wav": case "flac": case "aac": case "ogg":
                    return FileUpload.FileType.audio;
                case "pdf": case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx": case "txt":
                    return FileUpload.FileType.document;
                default:
                    return FileUpload.FileType.other;
            }
        }
        
        return FileUpload.FileType.other;
    }

    /**
     * 压缩图片
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 如果原图尺寸小于最大尺寸，直接返回
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }
        
        // 计算缩放比例
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 创建缩放后的图片
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // 设置高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
}