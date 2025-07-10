-- 为file_uploads表添加文件标签和过期时间字段
-- 用于支持临时文件和永久文件的生命周期管理
-- 版本: V20250112_001
-- 创建时间: 2025-01-12

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 检查并添加file_tag字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'file_tag';

SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE file_uploads ADD COLUMN file_tag VARCHAR(20) NOT NULL DEFAULT ''TEMPORARY'' COMMENT ''文件标签：TEMPORARY-临时文件，PERMANENT-永久文件''', 
  'SELECT ''Column file_tag already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加expires_at字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'expires_at';

SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE file_uploads ADD COLUMN expires_at DATETIME NULL COMMENT ''文件过期时间，仅对临时文件有效''', 
  'SELECT ''Column expires_at already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为新字段添加索引以优化查询性能
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_file_tag';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_file_tag ON file_uploads(file_tag)', 
  'SELECT ''Index idx_file_tag already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为expires_at字段添加索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_expires_at';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_expires_at ON file_uploads(expires_at)', 
  'SELECT ''Index idx_expires_at already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为临时文件查询添加复合索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_file_tag_expires_at';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_file_tag_expires_at ON file_uploads(file_tag, expires_at)', 
  'SELECT ''Index idx_file_tag_expires_at already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为桶名称和文件标签添加复合索引（用于MinIO生命周期规则查询）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_bucket_file_tag_expires';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_bucket_file_tag_expires ON file_uploads(bucket_name, file_tag, expires_at)', 
  'SELECT ''Index idx_bucket_file_tag_expires already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

-- 迁移完成提示
SELECT 'File tag and expires_at fields migration completed successfully' as status;