package com.im.imcommunicationsystem.user.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * MinIO对象存储服务接口
 * 提供文件上传、下载、删除等基础操作
 */
public interface MinioService {

    /**
     * 创建存储桶（如果不存在）
     *
     * @param bucketName 存储桶名称
     * @return 是否创建成功
     */
    boolean createBucketIfNotExists(String bucketName);

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 上传文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param file 文件
     * @return 是否上传成功
     */
    boolean uploadFile(String bucketName, String objectKey, MultipartFile file);

    /**
     * 上传文件流
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param size 文件大小
     * @return 是否上传成功
     */
    boolean uploadFile(String bucketName, String objectKey, InputStream inputStream, String contentType, long size);

    /**
     * 上传文件并设置元数据
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param file 文件
     * @param metadata 元数据
     * @return 是否上传成功
     */
    boolean uploadFileWithMetadata(String bucketName, String objectKey, MultipartFile file, Map<String, String> metadata);

    /**
     * 上传文件并设置标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param file 文件
     * @param tags 标签
     * @return 是否上传成功
     */
    boolean uploadFileWithTags(String bucketName, String objectKey, MultipartFile file, Map<String, String> tags);

    /**
     * 上传文件并设置元数据和标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param file 文件
     * @param metadata 元数据
     * @param tags 标签
     * @return 是否上传成功
     */
    boolean uploadFileWithMetadataAndTags(String bucketName, String objectKey, MultipartFile file, 
                                          Map<String, String> metadata, Map<String, String> tags);

    /**
     * 为已存在的文件设置标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param tags 标签
     * @return 是否设置成功
     */
    boolean setObjectTags(String bucketName, String objectKey, Map<String, String> tags);

    /**
     * 获取文件的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 标签映射
     */
    Map<String, String> getObjectTags(String bucketName, String objectKey);

    /**
     * 下载文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 文件输入流
     */
    InputStream downloadFile(String bucketName, String objectKey);

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 是否删除成功
     */
    boolean deleteFile(String bucketName, String objectKey);

    /**
     * 检查文件是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 是否存在
     */
    boolean fileExists(String bucketName, String objectKey);

    /**
     * 获取文件信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 文件信息
     */
    Map<String, Object> getFileInfo(String bucketName, String objectKey);

    /**
     * 获取预签名上传URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String getPresignedUploadUrl(String bucketName, String objectKey, int expireSeconds);

    /**
     * 获取预签名下载URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String getPresignedDownloadUrl(String bucketName, String objectKey, int expireSeconds);

    /**
     * 获取文件访问URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键（文件路径）
     * @return 文件访问URL
     */
    String getFileUrl(String bucketName, String objectKey);

    /**
     * 复制文件
     *
     * @param sourceBucket 源存储桶
     * @param sourceObjectKey 源对象键
     * @param targetBucket 目标存储桶
     * @param targetObjectKey 目标对象键
     * @return 是否复制成功
     */
    boolean copyFile(String sourceBucket, String sourceObjectKey, String targetBucket, String targetObjectKey);

    /**
     * 初始化所有必要的存储桶
     *
     * @return 是否初始化成功
     */
    boolean initializeBuckets();
}