-- 修复file_type字段类型不匹配问题
-- 将ENUM类型改为VARCHAR类型以匹配Java实体的@Enumerated(EnumType.STRING)注解

-- 检查file_type字段当前的数据类型
SET @column_type = '';
SELECT DATA_TYPE INTO @column_type
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'file_type';

-- 如果当前是ENUM类型，则修改为VARCHAR类型
SET @sql = IF(@column_type = 'enum', 
  'ALTER TABLE file_uploads MODIFY COLUMN file_type VARCHAR(20) NOT NULL DEFAULT ''other'' COMMENT ''文件类型分类：image-图片，video-视频，audio-音频，document-文档，other-其他''', 
  'SELECT ''Column file_type is already VARCHAR type'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证数据完整性，确保所有file_type值都是有效的
UPDATE file_uploads 
SET file_type = 'other' 
WHERE file_type NOT IN ('image', 'video', 'audio', 'document', 'other');

-- 添加CHECK约束确保file_type只能是有效值（如果不存在）
SET @constraint_exists = 0;
SELECT COUNT(*) INTO @constraint_exists 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND CONSTRAINT_NAME = 'chk_file_uploads_file_type';

SET @sql = IF(@constraint_exists = 0, 
  'ALTER TABLE file_uploads ADD CONSTRAINT chk_file_uploads_file_type CHECK (file_type IN (''image'', ''video'', ''audio'', ''document'', ''other''))', 
  'SELECT ''Constraint chk_file_uploads_file_type already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 同时修改storage_type字段类型以保持一致性
SET @storage_column_type = '';
SELECT DATA_TYPE INTO @storage_column_type
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'storage_type';

SET @sql = IF(@storage_column_type = 'enum', 
  'ALTER TABLE file_uploads MODIFY COLUMN storage_type VARCHAR(20) NOT NULL DEFAULT ''minio'' COMMENT ''存储类型：local-本地存储，minio-MinIO对象存储，s3-AWS S3，oss-阿里云OSS''', 
  'SELECT ''Column storage_type is already VARCHAR type'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证storage_type数据完整性
UPDATE file_uploads 
SET storage_type = 'minio' 
WHERE storage_type NOT IN ('local', 'minio', 's3', 'oss');

-- 添加storage_type的CHECK约束（如果不存在）
SET @storage_constraint_exists = 0;
SELECT COUNT(*) INTO @storage_constraint_exists 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND CONSTRAINT_NAME = 'chk_file_uploads_storage_type';

SET @sql = IF(@storage_constraint_exists = 0, 
  'ALTER TABLE file_uploads ADD CONSTRAINT chk_file_uploads_storage_type CHECK (storage_type IN (''local'', ''minio'', ''s3'', ''oss''))', 
  'SELECT ''Constraint chk_file_uploads_storage_type already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 输出修复结果
SELECT 'file_type和storage_type字段类型修复完成' as message;