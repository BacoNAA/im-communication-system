package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 媒体文件上传请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadRequest {

    /**
     * 上传的文件
     */
    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    /**
     * 文件类型（image, video, audio, document等）
     */
    @Size(max = 50, message = "文件类型不能超过50个字符")
    private String fileType;

    /**
     * 文件描述
     */
    @Size(max = 500, message = "文件描述不能超过500个字符")
    private String description;

    /**
     * 是否压缩（针对图片和视频）
     */
    private Boolean compress;

    /**
     * 压缩质量（0-100）
     */
    private Integer quality;

    /**
     * 检查是否需要压缩
     * 
     * @return 是否需要压缩
     */
    public boolean shouldCompress() {
        return compress != null && compress;
    }

    /**
     * 获取压缩质量，默认为80
     * 
     * @return 压缩质量
     */
    public int getCompressionQuality() {
        return quality != null && quality > 0 && quality <= 100 ? quality : 80;
    }
}