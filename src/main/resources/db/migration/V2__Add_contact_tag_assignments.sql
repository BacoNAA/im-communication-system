-- 创建联系人标签分配表
-- 用于建立联系人和标签的多对多关系
-- 注意：此表已通过JPA自动建表创建，此脚本用于Flyway版本管理

-- 检查表是否存在，如果不存在则创建（兼容JPA自动建表）
CREATE TABLE IF NOT EXISTS `contact_tag_assignments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分配记录唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME(6) NOT NULL COMMENT '分配时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人标签分配表';

-- 添加索引（如果不存在）
ALTER TABLE `contact_tag_assignments` 
ADD UNIQUE KEY IF NOT EXISTS `uk_contact_tag_assignments` (`user_id`, `friend_id`, `tag_id`),
ADD KEY IF NOT EXISTS `idx_contact_tag_assignments_user_id` (`user_id`),
ADD KEY IF NOT EXISTS `idx_contact_tag_assignments_friend_id` (`friend_id`),
ADD KEY IF NOT EXISTS `idx_contact_tag_assignments_tag_id` (`tag_id`),
ADD KEY IF NOT EXISTS `idx_contact_tag_assignments_created_at` (`created_at`);

-- 添加外键约束（如果不存在）
ALTER TABLE `contact_tag_assignments`
ADD CONSTRAINT IF NOT EXISTS `fk_contact_tag_assignments_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
ADD CONSTRAINT IF NOT EXISTS `fk_contact_tag_assignments_friend_id` FOREIGN KEY (`friend_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
ADD CONSTRAINT IF NOT EXISTS `fk_contact_tag_assignments_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `contact_tags` (`id`) ON DELETE CASCADE;