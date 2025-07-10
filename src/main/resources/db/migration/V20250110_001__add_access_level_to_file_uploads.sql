-- 为file_uploads表添加access_level字段
-- 用于支持公开和私有文件的分离管理

-- 检查并添加access_level字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'access_level';

SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE file_uploads ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT ''PRIVATE''', 
  'SELECT ''Column access_level already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加字段注释
ALTER TABLE file_uploads MODIFY COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '文件访问级别：PUBLIC-公开访问，PRIVATE-私有访问，RESTRICTED-受限访问';

-- 根据现有的is_public字段设置access_level的值
UPDATE file_uploads 
SET access_level = CASE 
    WHEN is_public = true THEN 'PUBLIC'
    ELSE 'PRIVATE'
END
WHERE access_level = 'PRIVATE';

-- 为access_level字段创建索引以提高查询性能（如果不存在）
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_file_uploads_access_level';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_file_uploads_access_level ON file_uploads(access_level)', 
  'SELECT ''Index idx_file_uploads_access_level already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建其他索引
SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_file_uploads_user_access_level';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_file_uploads_user_access_level ON file_uploads(user_id, access_level)', 
  'SELECT ''Index idx_file_uploads_user_access_level already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = 0;
SELECT COUNT(*) INTO @index_exists 
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND INDEX_NAME = 'idx_file_uploads_type_access_level';

SET @sql = IF(@index_exists = 0, 
  'CREATE INDEX idx_file_uploads_type_access_level ON file_uploads(file_type, access_level)', 
  'SELECT ''Index idx_file_uploads_type_access_level already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加约束确保access_level只能是有效值（如果不存在）
SET @constraint_exists = 0;
SELECT COUNT(*) INTO @constraint_exists 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'file_uploads' 
  AND CONSTRAINT_NAME = 'chk_file_uploads_access_level';

SET @sql = IF(@constraint_exists = 0, 
  'ALTER TABLE file_uploads ADD CONSTRAINT chk_file_uploads_access_level CHECK (access_level IN (''PUBLIC'', ''PRIVATE'', ''RESTRICTED''))', 
  'SELECT ''Constraint chk_file_uploads_access_level already exists'' as message');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为了向后兼容，保留is_public字段，但标记为已废弃
ALTER TABLE file_uploads MODIFY COLUMN is_public BOOLEAN DEFAULT false COMMENT '是否公开访问（已废弃，请使用access_level字段）';

-- 创建视图以便于查询公开文件
CREATE OR REPLACE VIEW public_files AS
SELECT * FROM file_uploads 
WHERE access_level = 'PUBLIC' AND is_deleted = false;

-- 创建视图以便于查询私有文件
CREATE OR REPLACE VIEW private_files AS
SELECT * FROM file_uploads 
WHERE access_level = 'PRIVATE' AND is_deleted = false;

-- MySQL不支持视图注释，在视图定义中已包含说明