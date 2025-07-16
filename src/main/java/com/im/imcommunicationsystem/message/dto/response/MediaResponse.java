package com.im.imcommunicationsystem.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 媒体文件响应类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 缩略图URL（如果适用）
     */
    private String thumbnailUrl;

    /**
     * 文件描述
     */
    private String description;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 是否已压缩
     */
    private Boolean compressed;

    /**
     * 文件状态（uploading, completed, failed等）
     */
    private String status;

    /**
     * 获取格式化的文件大小
     * 
     * @return 格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "未知";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 检查是否为图片文件
     * 
     * @return 是否为图片
     */
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 检查是否为视频文件
     * 
     * @return 是否为视频
     */
    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 检查是否为音频文件
     * 
     * @return 是否为音频
     */
    public boolean isAudio() {
        return mimeType != null && mimeType.startsWith("audio/");
    }
}