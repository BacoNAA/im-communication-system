-- 更新 conversations 表结构以匹配 Conversation 实体类
-- 版本: V20250113_001
-- 创建时间: 2025-01-13
-- 描述: 添加缺少的字段以匹配 Conversation 实体类的完整定义

-- 添加 name 字段（会话名称）
ALTER TABLE `conversations` 
ADD COLUMN `name` VARCHAR(100) NULL COMMENT '会话名称' AFTER `type`;

-- 添加 description 字段（会话描述）
ALTER TABLE `conversations` 
ADD COLUMN `description` VARCHAR(500) NULL COMMENT '会话描述' AFTER `name`;

-- 添加 avatar_url 字段（会话头像URL）
ALTER TABLE `conversations` 
ADD COLUMN `avatar_url` VARCHAR(500) NULL COMMENT '会话头像URL' AFTER `description`;

-- 添加 created_by 字段（创建者ID）
ALTER TABLE `conversations` 
ADD COLUMN `created_by` BIGINT NOT NULL COMMENT '创建者ID' AFTER `avatar_url`;

-- 添加 last_active_at 字段（最后活跃时间）
ALTER TABLE `conversations` 
ADD COLUMN `last_active_at` TIMESTAMP NULL COMMENT '最后活跃时间' AFTER `created_by`;

-- 添加 last_message_id 字段（最后一条消息ID）
ALTER TABLE `conversations` 
ADD COLUMN `last_message_id` BIGINT NULL COMMENT '最后一条消息ID' AFTER `last_active_at`;

-- 添加 deleted 字段（是否已删除）
ALTER TABLE `conversations` 
ADD COLUMN `deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已删除' AFTER `last_message_id`;

-- 添加 settings 字段（会话设置）
ALTER TABLE `conversations` 
ADD COLUMN `settings` TEXT NULL COMMENT '会话设置（JSON格式）' AFTER `deleted`;

-- 添加 metadata 字段（会话元数据）
ALTER TABLE `conversations` 
ADD COLUMN `metadata` TEXT NULL COMMENT '会话元数据（JSON格式）' AFTER `settings`;

-- 添加 updated_at 字段（更新时间）
ALTER TABLE `conversations` 
ADD COLUMN `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- 添加 deleted_at 字段（删除时间）
ALTER TABLE `conversations` 
ADD COLUMN `deleted_at` TIMESTAMP NULL COMMENT '删除时间' AFTER `updated_at`;

-- 更新 type 字段以匹配实体类中的枚举
ALTER TABLE `conversations` 
CHANGE COLUMN `type` `conversation_type` ENUM('PRIVATE', 'GROUP', 'SYSTEM') NOT NULL COMMENT '会话类型';

-- 删除不再需要的 related_group_id 字段（如果存在）
-- ALTER TABLE `conversations` DROP COLUMN `related_group_id`;

-- 添加索引以提高查询性能
CREATE INDEX `idx_conversations_created_by` ON `conversations` (`created_by`);
CREATE INDEX `idx_conversations_last_active` ON `conversations` (`last_active_at`);
CREATE INDEX `idx_conversations_deleted` ON `conversations` (`deleted`);

-- 添加外键约束
ALTER TABLE `conversations` 
ADD CONSTRAINT `fk_conversations_created_by` 
FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE CASCADE;