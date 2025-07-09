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
}