package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.entity.FileUpload;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 公开文件上传服务接口
 * 专门处理公开访问的文件（如头像、公开图片等）
 */
public interface PublicFileUploadService {

    /**
     * 上传公开文件
     * 
     * @param file 文件
     * @param userId 用户ID
     * @return 文件URL
     */
    String uploadFile(MultipartFile file, Long userId);

    /**
     * 上传公开图片（含压缩处理）
     * 
     * @param file 图片文件
     * @param userId 用户ID
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 图片URL
     */
    String uploadImage(MultipartFile file, Long userId, int maxWidth, int maxHeight);

    /**
     * 上传头像
     * 
     * @param file 头像文件
     * @param userId 用户ID
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 删除公开文件（软删除）
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId, Long userId);

    /**
     * 物理删除公开文件
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean physicalDeleteFile(Long fileId, Long userId);

    /**
     * 获取用户的公开文件列表
     * 
     * @param userId 用户ID
     * @param fileType 文件类型（可选）
     * @return 文件列表
     */
    List<FileUpload> getUserPublicFiles(Long userId, FileUpload.FileType fileType);

    /**
     * 根据文件ID获取公开文件信息
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileUpload getPublicFileById(Long fileId);

    /**
     * 检查文件是否为公开文件
     * 
     * @param fileId 文件ID
     * @return 是否为公开文件
     */
    boolean isPublicFile(Long fileId);

    /**
     * 获取文件的公开访问URL
     * 
     * @param fileId 文件ID
     * @return 公开访问URL
     */
    String getPublicFileUrl(Long fileId);
}