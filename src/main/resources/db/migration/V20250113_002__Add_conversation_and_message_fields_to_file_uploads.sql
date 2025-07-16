-- 为file_uploads表添加conversation_id和message_id字段
-- 版本: V20250113_002
-- 创建时间: 2025-01-13
-- 描述: 添加会话ID和消息ID字段，支持文件与会话和消息的关联

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 检查conversation_id字段是否存在，如果不存在则添加
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND COLUMN_NAME = 'conversation_id') = 0,
    'ALTER TABLE file_uploads ADD COLUMN conversation_id BIGINT NULL COMMENT "会话ID（可选，如果是在会话中发送的媒体文件）"',
    'SELECT "Column conversation_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查message_id字段是否存在，如果不存在则添加
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND COLUMN_NAME = 'message_id') = 0,
    'ALTER TABLE file_uploads ADD COLUMN message_id BIGINT NULL COMMENT "消息ID（可选，如果是消息中的媒体文件）"',
    'SELECT "Column message_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查metadata字段是否存在，如果不存在则添加
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND COLUMN_NAME = 'metadata') = 0,
    'ALTER TABLE file_uploads ADD COLUMN metadata TEXT NULL COMMENT "文件元数据（JSON格式）"',
    'SELECT "Column metadata already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加conversation_id索引（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND INDEX_NAME = 'idx_file_uploads_conversation_id') = 0,
    'CREATE INDEX idx_file_uploads_conversation_id ON file_uploads(conversation_id)',
    'SELECT "Index idx_file_uploads_conversation_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加message_id索引（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND INDEX_NAME = 'idx_file_uploads_message_id') = 0,
    'CREATE INDEX idx_file_uploads_message_id ON file_uploads(message_id)',
    'SELECT "Index idx_file_uploads_message_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加组合索引：conversation_id + message_id（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'file_uploads' 
     AND INDEX_NAME = 'idx_file_uploads_conversation_message') = 0,
    'CREATE INDEX idx_file_uploads_conversation_message ON file_uploads(conversation_id, message_id)',
    'SELECT "Index idx_file_uploads_conversation_message already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 迁移完成
-- 添加的字段：
-- 1. conversation_id: 会话ID，用于关联文件与会话
-- 2. message_id: 消息ID，用于关联文件与消息
-- 3. metadata: 文件元数据，JSON格式存储额外信息
-- 添加的索引：
-- 1. idx_file_uploads_conversation_id: conversation_id索引
-- 2. idx_file_uploads_message_id: message_id索引
-- 3. idx_file_uploads_conversation_message: conversation_id + message_id组合索引