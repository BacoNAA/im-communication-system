-- 为用户表添加是否被封禁的列
ALTER TABLE `users`
ADD COLUMN `is_banned` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '用户是否被封禁' AFTER `updated_at`,
ADD COLUMN `banned_reason` VARCHAR(255) NULL COMMENT '封禁原因' AFTER `is_banned`,
ADD COLUMN `banned_until` TIMESTAMP NULL COMMENT '封禁截止时间' AFTER `banned_reason`,
ADD INDEX `idx_users_is_banned` (`is_banned`);

-- 为群组表添加是否被封禁的列
ALTER TABLE `groups`
ADD COLUMN `is_banned` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '群组是否被封禁' AFTER `updated_at`,
ADD COLUMN `banned_reason` VARCHAR(255) NULL COMMENT '封禁原因' AFTER `is_banned`,
ADD COLUMN `banned_until` TIMESTAMP NULL COMMENT '封禁截止时间' AFTER `banned_reason`,
ADD INDEX `idx_groups_is_banned` (`is_banned`); 