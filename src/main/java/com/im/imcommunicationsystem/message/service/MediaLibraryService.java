package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.dto.request.MediaUploadRequest;
import com.im.imcommunicationsystem.message.dto.response.MediaResponse;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 媒体库服务接口
 * 定义媒体文件管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface MediaLibraryService {

    /**
     * 获取媒体库
     * 
     * @param conversationId 会话ID（可选）
     * @param mediaType 媒体类型（可选）
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 媒体列表
     */
    Page<MediaResponse> getMediaLibrary(Long conversationId, String mediaType, Pageable pageable, Long userId);

    /**
     * 上传媒体文件
     * 
     * @param file 文件
     * @param request 上传请求
     * @param userId 用户ID
     * @return 媒体响应
     */
    MediaResponse uploadMedia(MultipartFile file, MediaUploadRequest request, Long userId);

    /**
     * 下载媒体文件
     * 
     * @param mediaId 媒体ID
     * @param userId 用户ID
     * @return 文件字节数组
     */
    byte[] downloadMedia(Long mediaId, Long userId);

    /**
     * 根据ID获取媒体文件
     * 
     * @param mediaId 媒体ID
     * @return 媒体文件实体（来自用户模块的FileUpload）
     */
    FileUpload getMediaFileById(Long mediaId);

    /**
     * 删除媒体文件
     * 
     * @param mediaId 媒体ID
     * @param userId 用户ID
     */
    void deleteMediaFile(Long mediaId, Long userId);

    /**
     * 获取会话中的媒体文件
     * 
     * @param conversationId 会话ID
     * @param mediaType 媒体类型
     * @return 媒体文件列表（来自用户模块的FileUpload）
     */
    List<FileUpload> getMediaFilesByConversation(Long conversationId, String mediaType);

    /**
     * 获取用户上传的媒体文件
     * 
     * @param userId 用户ID
     * @param mediaType 媒体类型
     * @return 媒体文件列表（来自用户模块的FileUpload）
     */
    List<FileUpload> getMediaFilesByUser(Long userId, String mediaType);

    /**
     * 验证文件类型
     * 
     * @param file 文件
     * @return 是否有效
     */
    boolean validateFileType(MultipartFile file);

    /**
     * 验证文件大小
     * 
     * @param file 文件
     * @return 是否有效
     */
    boolean validateFileSize(MultipartFile file);

    /**
     * 生成文件缩略图
     * 
     * @param mediaId 媒体ID
     * @return 缩略图字节数组
     */
    byte[] generateThumbnail(Long mediaId);
}