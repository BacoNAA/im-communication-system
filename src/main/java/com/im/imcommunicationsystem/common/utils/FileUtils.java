package com.im.imcommunicationsystem.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件工具类
 * 用于处理文件上传、下载、存储和管理
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class FileUtils {

    /**
     * 文件上传根路径
     */
    @Value("${file.upload.path:/uploads}")
    private String uploadPath;

    /**
     * 文件访问URL前缀
     */
    @Value("${file.access.url-prefix:/api/files}")
    private String urlPrefix;

    /**
     * 最大文件大小（字节）
     */
    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize;

    /**
     * 允许的图片文件类型
     */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /**
     * 允许的视频文件类型
     */
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm"
    );

    /**
     * 允许的音频文件类型
     */
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mp3", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/m4a"
    );

    /**
     * 允许的文档文件类型
     */
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv"
    );

    /**
     * 文件类型枚举
     */
    public enum FileType {
        IMAGE("images", ALLOWED_IMAGE_TYPES),
        VIDEO("videos", ALLOWED_VIDEO_TYPES),
        AUDIO("audios", ALLOWED_AUDIO_TYPES),
        DOCUMENT("documents", ALLOWED_DOCUMENT_TYPES),
        OTHER("others", Collections.emptySet());

        private final String directory;
        private final Set<String> allowedTypes;

        FileType(String directory, Set<String> allowedTypes) {
            this.directory = directory;
            this.allowedTypes = allowedTypes;
        }

        public String getDirectory() {
            return directory;
        }

        public Set<String> getAllowedTypes() {
            return allowedTypes;
        }

        public static FileType fromContentType(String contentType) {
            if (contentType == null) return OTHER;
            
            for (FileType type : values()) {
                if (type.getAllowedTypes().contains(contentType.toLowerCase())) {
                    return type;
                }
            }
            return OTHER;
        }
    }

    /**
     * 文件上传结果
     */
    public static class UploadResult {
        private String fileName;
        private String originalName;
        private String filePath;
        private String fileUrl;
        private String contentType;
        private long fileSize;
        private String md5Hash;
        private FileType fileType;

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getMd5Hash() { return md5Hash; }
        public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
        
        public FileType getFileType() { return fileType; }
        public void setFileType(FileType fileType) { this.fileType = fileType; }
    }

    /**
     * 上传文件
     * 
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 上传结果
     * @throws IOException IO异常
     */
    public UploadResult uploadFile(MultipartFile file, Long userId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 确定文件类型
        FileType fileType = FileType.fromContentType(file.getContentType());
        
        // 生成文件名
        String fileName = generateFileName(file.getOriginalFilename());
        
        // 构建文件路径
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String relativePath = String.format("%s/%s/%s", fileType.getDirectory(), datePath, fileName);
        String fullPath = Paths.get(uploadPath, relativePath).toString();
        
        // 创建目录
        Path directory = Paths.get(fullPath).getParent();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        // 保存文件
        File targetFile = new File(fullPath);
        file.transferTo(targetFile);
        
        // 计算MD5
        String md5Hash = calculateMD5(targetFile);
        
        // 构建结果
        UploadResult result = new UploadResult();
        result.setFileName(fileName);
        result.setOriginalName(file.getOriginalFilename());
        result.setFilePath(relativePath);
        result.setFileUrl(urlPrefix + "/" + relativePath);
        result.setContentType(file.getContentType());
        result.setFileSize(file.getSize());
        result.setMd5Hash(md5Hash);
        result.setFileType(fileType);
        
        log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), relativePath);
        
        return result;
    }

    /**
     * 验证文件
     * 
     * @param file 文件
     * @throws IllegalArgumentException 验证失败异常
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超出限制: " + formatFileSize(maxFileSize));
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("无法确定文件类型");
        }
        
        // 检查文件类型是否被允许
        FileType fileType = FileType.fromContentType(contentType);
        if (fileType == FileType.OTHER) {
            // 如果是OTHER类型，检查是否在所有允许的类型中
            boolean isAllowed = ALLOWED_IMAGE_TYPES.contains(contentType) ||
                               ALLOWED_VIDEO_TYPES.contains(contentType) ||
                               ALLOWED_AUDIO_TYPES.contains(contentType) ||
                               ALLOWED_DOCUMENT_TYPES.contains(contentType);
            
            if (!isAllowed) {
                throw new IllegalArgumentException("不支持的文件类型: " + contentType);
            }
        }
    }

    /**
     * 生成唯一文件名
     * 
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 扩展名
     */
    public String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 计算文件MD5值
     * 
     * @param file 文件
     * @return MD5值
     * @throws IOException IO异常
     */
    public String calculateMD5(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("计算MD5失败", e);
        }
    }

    /**
     * 删除文件
     * 
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadPath, filePath);
            boolean deleted = Files.deleteIfExists(path);
            
            if (deleted) {
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件不存在: {}", filePath);
            }
            
            return deleted;
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * 
     * @param filePath 文件路径
     * @return 是否存在
     */
    public boolean fileExists(String filePath) {
        Path path = Paths.get(uploadPath, filePath);
        return Files.exists(path);
    }

    /**
     * 获取文件大小
     * 
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(uploadPath, filePath);
            return Files.size(path);
        } catch (IOException e) {
            log.error("获取文件大小失败: {}", filePath, e);
            return 0;
        }
    }

    /**
     * 格式化文件大小
     * 
     * @param size 文件大小（字节）
     * @return 格式化后的大小
     */
    public String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取文件输入流
     * 
     * @param filePath 文件路径
     * @return 文件输入流
     * @throws IOException IO异常
     */
    public InputStream getFileInputStream(String filePath) throws IOException {
        Path path = Paths.get(uploadPath, filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return Files.newInputStream(path);
    }

    /**
     * 文件工具使用说明：
     * 
     * 1. 文件存储结构：
     *    /uploads/
     *    ├── images/2024/01/01/uuid.jpg
     *    ├── videos/2024/01/01/uuid.mp4
     *    ├── audios/2024/01/01/uuid.mp3
     *    ├── documents/2024/01/01/uuid.pdf
     *    └── others/2024/01/01/uuid.bin
     * 
     * 2. 支持的文件类型：
     *    - 图片: JPEG, PNG, GIF, WebP, BMP
     *    - 视频: MP4, AVI, MOV, WMV, FLV, WebM
     *    - 音频: MP3, WAV, OGG, AAC, FLAC, M4A
     *    - 文档: PDF, Word, Excel, PowerPoint, TXT, CSV
     * 
     * 3. 安全特性：
     *    - 文件大小限制
     *    - 文件类型验证
     *    - 唯一文件名生成
     *    - MD5校验
     * 
     * 4. 使用示例：
     *    // 上传文件
     *    UploadResult result = uploadFile(multipartFile, userId);
     *    
     *    // 删除文件
     *    boolean deleted = deleteFile(result.getFilePath());
     *    
     *    // 检查文件存在
     *    boolean exists = fileExists(filePath);
     * 
     * 5. 配置参数：
     *    - file.upload.path: 文件上传根路径
     *    - file.access.url-prefix: 文件访问URL前缀
     *    - file.upload.max-size: 最大文件大小
     */

}