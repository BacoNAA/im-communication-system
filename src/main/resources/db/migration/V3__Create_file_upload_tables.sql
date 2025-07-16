-- 文件上传相关表的数据库迁移脚本
-- 版本: V2.0
-- 创建时间: 2024-01-02

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 文件上传相关表
-- ========================================

-- 文件上传记录表
CREATE TABLE `file_uploads` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `content_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    `file_type` ENUM('image', 'video', 'audio', 'document', 'other') NOT NULL DEFAULT 'other' COMMENT '文件类型分类',
    `md5_hash` VARCHAR(32) NULL COMMENT '文件MD5哈希值',
    `storage_type` ENUM('local', 'minio', 's3', 'oss') NOT NULL DEFAULT 'minio' COMMENT '存储类型',
    `bucket_name` VARCHAR(100) NULL COMMENT '存储桶名称',
    `object_key` VARCHAR(500) NULL COMMENT '对象存储键',
    `width` INT NULL COMMENT '图片/视频宽度',
    `height` INT NULL COMMENT '图片/视频高度',
    `duration` INT NULL COMMENT '音频/视频时长（秒）',
    `thumbnail_url` VARCHAR(500) NULL COMMENT '缩略图URL',
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开访问',
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已删除',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` TIMESTAMP NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_uploads_user_id` (`user_id`),
    KEY `idx_file_uploads_file_type` (`file_type`),
    KEY `idx_file_uploads_storage_type` (`storage_type`),
    KEY `idx_file_uploads_md5_hash` (`md5_hash`),
    KEY `idx_file_uploads_is_deleted` (`is_deleted`),
    KEY `idx_file_uploads_created_at` (`created_at`),
    CONSTRAINT `fk_file_uploads_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- 文件分享记录表
CREATE TABLE `file_shares` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分享记录唯一标识',
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `user_id` BIGINT NOT NULL COMMENT '分享用户ID',
    `share_code` VARCHAR(32) NOT NULL COMMENT '分享码',
    `share_url` VARCHAR(500) NOT NULL COMMENT '分享链接',
    `password` VARCHAR(20) NULL COMMENT '提取密码',
    `expires_at` TIMESTAMP NULL COMMENT '过期时间',
    `download_count` INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    `max_download_count` INT NULL COMMENT '最大下载次数',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有效',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_shares_share_code` (`share_code`),
    KEY `idx_file_shares_file_id` (`file_id`),
    KEY `idx_file_shares_user_id` (`user_id`),
    KEY `idx_file_shares_is_active` (`is_active`),
    KEY `idx_file_shares_expires_at` (`expires_at`),
    KEY `idx_file_shares_created_at` (`created_at`),
    CONSTRAINT `fk_file_shares_file_id` FOREIGN KEY (`file_id`) REFERENCES `file_uploads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_file_shares_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享记录表';

-- 文件下载记录表
CREATE TABLE `file_downloads` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '下载记录唯一标识',
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `user_id` BIGINT NULL COMMENT '下载用户ID（可为空，支持匿名下载）',
    `share_id` BIGINT NULL COMMENT '分享记录ID（通过分享链接下载时）',
    `ip_address` VARCHAR(45) NULL COMMENT '下载IP地址',
    `user_agent` VARCHAR(500) NULL COMMENT '用户代理',
    `download_size` BIGINT NOT NULL DEFAULT 0 COMMENT '下载字节数',
    `is_completed` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否下载完成',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_downloads_file_id` (`file_id`),
    KEY `idx_file_downloads_user_id` (`user_id`),
    KEY `idx_file_downloads_share_id` (`share_id`),
    KEY `idx_file_downloads_ip_address` (`ip_address`),
    KEY `idx_file_downloads_created_at` (`created_at`),
    CONSTRAINT `fk_file_downloads_file_id` FOREIGN KEY (`file_id`) REFERENCES `file_uploads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_file_downloads_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_file_downloads_share_id` FOREIGN KEY (`share_id`) REFERENCES `file_shares` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件下载记录表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 文件上传相关表创建完成
-- 总计创建了3个文件上传相关表：
-- 1. file_uploads: 文件上传记录表，存储文件的基本信息和元数据
-- 2. file_shares: 文件分享记录表，支持文件分享功能
-- 3. file_downloads: 文件下载记录表，记录文件下载统计信息
-- 所有表都使用InnoDB存储引擎，支持事务和外键约束
-- 字符集为utf8mb4，支持Emoji表情和多语言
-- 已添加适当的索引以优化查询性能
-- 外键约束确保数据完整性