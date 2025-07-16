-- 更新messages表结构以匹配Message实体类
-- 版本: V20250113_003
-- 创建时间: 2025-01-13
-- 描述: 添加缺失的字段并更新现有字段以匹配Message实体类定义

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 添加message_type字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'message_type') = 0,
    'ALTER TABLE messages ADD COLUMN message_type VARCHAR(50) NOT NULL DEFAULT "TEXT" COMMENT "消息类型"',
    'SELECT "Column message_type already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 修改content字段类型为TEXT（如果是JSON）
SET @sql = IF(
    (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'content') = 'json',
    'ALTER TABLE messages MODIFY COLUMN content TEXT COMMENT "消息内容"',
    'SELECT "Column content type is already correct" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加media_file_id字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'media_file_id') = 0,
    'ALTER TABLE messages ADD COLUMN media_file_id BIGINT NULL COMMENT "媒体文件ID（如果是媒体消息，关联用户模块的FileUpload）"',
    'SELECT "Column media_file_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 添加reply_to_message_id字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'reply_to_message_id') = 0,
    'ALTER TABLE messages ADD COLUMN reply_to_message_id BIGINT NULL COMMENT "回复的消息ID"',
    'SELECT "Column reply_to_message_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 添加original_message_id字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'original_message_id') = 0,
    'ALTER TABLE messages ADD COLUMN original_message_id BIGINT NULL COMMENT "转发的原始消息ID"',
    'SELECT "Column original_message_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 添加status字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE messages ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT "SENT" COMMENT "消息状态"',
    'SELECT "Column status already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 添加edited字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'edited') = 0,
    'ALTER TABLE messages ADD COLUMN edited BOOLEAN NOT NULL DEFAULT FALSE COMMENT "是否已编辑"',
    'SELECT "Column edited already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8. 添加indexed字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'indexed') = 0,
    'ALTER TABLE messages ADD COLUMN indexed BOOLEAN NOT NULL DEFAULT FALSE COMMENT "是否已索引到搜索引擎"',
    'SELECT "Column indexed already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 9. 添加metadata字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'metadata') = 0,
    'ALTER TABLE messages ADD COLUMN metadata TEXT NULL COMMENT "消息元数据（JSON格式）"',
    'SELECT "Column metadata already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 10. 添加updated_at字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE messages ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT "更新时间"',
    'SELECT "Column updated_at already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 11. 添加deleted_at字段（如果不存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND COLUMN_NAME = 'deleted_at') = 0,
    'ALTER TABLE messages ADD COLUMN deleted_at TIMESTAMP NULL COMMENT "删除时间"',
    'SELECT "Column deleted_at already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 12. 添加相关索引
-- 添加message_type索引
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND INDEX_NAME = 'idx_message_type') = 0,
    'CREATE INDEX idx_message_type ON messages(message_type)',
    'SELECT "Index idx_message_type already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加status索引
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND INDEX_NAME = 'idx_status') = 0,
    'CREATE INDEX idx_status ON messages(status)',
    'SELECT "Index idx_status already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加reply_to_message_id索引
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND INDEX_NAME = 'idx_reply_to') = 0,
    'CREATE INDEX idx_reply_to ON messages(reply_to_message_id)',
    'SELECT "Index idx_reply_to already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加media_file_id索引
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'messages' 
     AND INDEX_NAME = 'idx_media_file_id') = 0,
    'CREATE INDEX idx_media_file_id ON messages(media_file_id)',
    'SELECT "Index idx_media_file_id already exists" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 迁移完成
-- 更新的字段：
-- 1. message_type: 消息类型
-- 2. content: 修改为TEXT类型
-- 3. media_file_id: 媒体文件ID
-- 4. reply_to_message_id: 回复消息ID
-- 5. original_message_id: 原始消息ID
-- 6. status: 消息状态
-- 7. edited: 是否已编辑
-- 8. indexed: 是否已索引
-- 9. metadata: 元数据
-- 10. updated_at: 更新时间
-- 11. deleted_at: 删除时间
-- 添加的索引：
-- 1. idx_message_type: message_type索引
-- 2. idx_status: status索引
-- 3. idx_reply_to: reply_to_message_id索引
-- 4. idx_media_file_id: media_file_id索引