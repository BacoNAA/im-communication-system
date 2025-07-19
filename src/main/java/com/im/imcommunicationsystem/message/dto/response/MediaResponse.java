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
     * 媒体文件ID
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
     * 文件URL（别名，与前端兼容）
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 压缩标志
     */
    private Boolean compressed;

    /**
     * 状态
     */
    private String status;
}