package com.im.imcommunicationsystem.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文件上传记录实体类
 * 对应数据库表：file_uploads
 */
@Entity
@Table(name = "file_uploads")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class FileUpload {

    /**
     * 文件唯一标识
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 上传用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /**
     * 存储文件名
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * 文件存储路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * 文件访问URL
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * 文件类型分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType = FileType.other;

    /**
     * 文件MD5哈希值
     */
    @Column(name = "md5_hash", length = 32)
    private String md5Hash;

    /**
     * 存储类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private StorageType storageType = StorageType.minio;

    /**
     * 存储桶名称
     */
    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    /**
     * 对象存储键
     */
    @Column(name = "object_key", length = 500)
    private String objectKey;

    /**
     * 图片/视频宽度
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 图片/视频高度
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 音频/视频时长（秒）
     */
    @Column(name = "duration")
    private Integer duration;

    /**
     * 缩略图URL
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * 文件访问级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel = AccessLevel.PRIVATE;
    
    /**
     * 是否公开访问（兼容性字段）
     */
    @Deprecated
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    /**
     * 是否已删除
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 上传时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 文件标签（用于生命周期管理）
     * TEMPORARY: 临时文件，会被自动清理
     * PERMANENT: 永久文件，需要手动管理
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_tag", nullable = false)
    private FileTag fileTag = FileTag.TEMPORARY;

    /**
     * 过期时间（仅对临时文件有效）
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 文件类型枚举
     */
    public enum FileType {
        image,
        video,
        audio,
        document,
        other;

        /**
         * 获取枚举值
         */
        public String getValue() {
            return this.name();
        }

        /**
         * 根据MIME类型判断文件类型
         */
        public static FileType fromContentType(String contentType) {
            if (contentType == null) {
                return other;
            }
            
            String type = contentType.toLowerCase();
            if (type.startsWith("image/")) {
                return image;
            } else if (type.startsWith("video/")) {
                return video;
            } else if (type.startsWith("audio/")) {
                return audio;
            } else if (type.contains("pdf") || type.contains("document") || 
                      type.contains("text") || type.contains("word") ||
                      type.contains("excel") || type.contains("powerpoint")) {
                return document;
            }
            return other;
        }
    }

    /**
     * 存储类型枚举
     */
    public enum StorageType {
        local,
        minio,
        s3,
        oss;

        /**
         * 获取枚举值
         */
        public String getValue() {
            return this.name();
        }
    }
    
    /**
     * 文件访问级别枚举
     */
    public enum AccessLevel {
        /**
         * 公开访问 - 任何人都可以访问
         */
        PUBLIC,
        
        /**
         * 私有访问 - 仅文件所有者可以访问
         */
        PRIVATE,
        
        /**
         * 受限访问 - 需要特定权限才能访问
         */
        RESTRICTED;
        
        /**
         * 获取枚举值
         */
        public String getValue() {
            return this.name();
        }
        
        /**
         * 判断是否为公开访问
         */
        public boolean isPublic() {
            return this == PUBLIC;
        }
        
        /**
         * 判断是否为私有访问
         */
        public boolean isPrivate() {
            return this == PRIVATE;
        }
    }
    
    /**
     * 文件标签枚举（用于生命周期管理）
     */
    public enum FileTag {
        /**
         * 临时文件 - 会被自动清理
         */
        TEMPORARY,
        
        /**
         * 永久文件 - 需要手动管理（如头像）
         */
        PERMANENT;
        
        /**
         * 获取枚举值
         */
        public String getValue() {
            return this.name();
        }
        
        /**
         * 判断是否为临时文件
         */
        public boolean isTemporary() {
            return this == TEMPORARY;
        }
        
        /**
         * 判断是否为永久文件
         */
        public boolean isPermanent() {
            return this == PERMANENT;
        }
    }
}