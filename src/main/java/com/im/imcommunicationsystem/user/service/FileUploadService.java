package com.im.imcommunicationsystem.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * 实现文件上传和处理的业务逻辑
 */
public interface FileUploadService {

    /**
     * 上传文件
     * 
     * @param file 文件
     * @param directory 目录
     * @return 文件URL
     */
    String uploadFile(MultipartFile file, String directory);

    /**
     * 上传图片（含压缩处理）
     * 
     * @param file 图片文件
     * @param directory 目录
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 图片URL
     */
    String uploadImage(MultipartFile file, String directory, int maxWidth, int maxHeight);

    /**
     * 删除文件
     * 
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * 验证文件类型
     * 
     * @param file 文件
     * @param allowedTypes 允许的文件类型
     * @return 是否有效
     */
    boolean validateFileType(MultipartFile file, String[] allowedTypes);

    /**
     * 生成文件名
     * 
     * @param originalFilename 原始文件名
     * @return 生成的文件名
     */
    String generateFileName(String originalFilename);

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 扩展名
     */
    String getFileExtension(String filename);

    /**
     * 获取用户文件列表（分页）
     * 
     * @param userId 用户ID
     * @param fileType 文件类型（可为null）
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    java.util.List<com.im.imcommunicationsystem.user.entity.FileUpload> getUserFiles(Long userId, com.im.imcommunicationsystem.user.entity.FileUpload.FileType fileType, int page, int size);

    /**
     * 获取用户文件统计信息
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getUserFileStats(Long userId);

    // ==================== 消息模块特有功能 ====================

    /**
     * 上传媒体文件（用于消息）
     * 
     * @param file 文件
     * @param uploaderId 上传者ID
     * @param conversationId 会话ID（可选）
     * @param messageId 消息ID（可选）
     * @return 文件上传记录
     */
    com.im.imcommunicationsystem.user.entity.FileUpload uploadMediaFile(MultipartFile file, Long uploaderId, Long conversationId, Long messageId);

    /**
     * 根据ID获取文件
     * 
     * @param fileId 文件ID
     * @return 文件记录
     */
    com.im.imcommunicationsystem.user.entity.FileUpload getFileById(Long fileId);

    /**
     * 根据会话ID获取媒体文件列表
     * 
     * @param conversationId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    java.util.List<com.im.imcommunicationsystem.user.entity.FileUpload> getFilesByConversationId(Long conversationId, int page, int size);

    /**
     * 根据消息ID获取媒体文件
     * 
     * @param messageId 消息ID
     * @return 文件记录
     */
    com.im.imcommunicationsystem.user.entity.FileUpload getFileByMessageId(Long messageId);

    /**
     * 根据上传者ID获取媒体文件列表
     * 
     * @param uploaderId 上传者ID
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    java.util.List<com.im.imcommunicationsystem.user.entity.FileUpload> getFilesByUploaderId(Long uploaderId, int page, int size);

    /**
     * 软删除文件
     * 
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean softDeleteFile(Long fileId);

    /**
     * 更新文件的会话和消息关联
     * 
     * @param fileId 文件ID
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 是否更新成功
     */
    boolean updateFileAssociation(Long fileId, Long conversationId, Long messageId);
}