-- 更新moments表结构
-- 注意: user_id列已存在，不需要重命名

-- 1. 添加media_type字段
ALTER TABLE `moments` 
    ADD COLUMN `media_type` ENUM('TEXT', 'IMAGE', 'VIDEO') NOT NULL DEFAULT 'TEXT' COMMENT '媒体类型' AFTER `content`;

-- 2. 修改content字段类型
ALTER TABLE `moments` 
    MODIFY COLUMN `content` VARCHAR(2000) NULL COMMENT '动态文字内容';

-- 3. 重命名media_json为media_urls
ALTER TABLE `moments` 
    CHANGE COLUMN `media_json` `media_urls` JSON NULL COMMENT '媒体文件URL列表（JSON格式）';

-- 4. 重命名visibility_json为visibility_type
ALTER TABLE `moments` 
    CHANGE COLUMN `visibility_json` `visibility_type` ENUM('PUBLIC', 'PRIVATE', 'CUSTOM') NOT NULL DEFAULT 'PUBLIC' COMMENT '可见性类型';

-- 5. 添加visibility_rules字段
ALTER TABLE `moments` 
    ADD COLUMN `visibility_rules` JSON NULL COMMENT '可见性规则（JSON格式）' AFTER `visibility_type`;

-- 6. 添加点赞数量字段
ALTER TABLE `moments` 
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数量' AFTER `visibility_rules`;

-- 7. 添加评论数量字段
ALTER TABLE `moments` 
    ADD COLUMN `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数量' AFTER `like_count`;

-- 8. 添加更新时间字段
ALTER TABLE `moments` 
    ADD COLUMN `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- 9. 添加媒体类型索引
ALTER TABLE `moments` 
    ADD INDEX `idx_moments_media_type` (`media_type`);

-- 修改moment_likes表结构
-- 1. 修改主键
ALTER TABLE `moment_likes` 
    DROP PRIMARY KEY,
    ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `uk_moment_likes_moment_user` (`moment_id`, `user_id`);

-- 修改moment_comments表结构
-- 1. 重命名author_id为user_id
ALTER TABLE `moment_comments` 
    CHANGE COLUMN `author_id` `user_id` BIGINT NOT NULL COMMENT '评论用户ID';

-- 2. 添加回复目标用户ID字段
ALTER TABLE `moment_comments` 
    ADD COLUMN `reply_to_user_id` BIGINT NULL COMMENT '回复目标用户ID' AFTER `parent_comment_id`;

-- 3. 添加是否私密评论字段
ALTER TABLE `moment_comments` 
    ADD COLUMN `is_private` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否私密评论' AFTER `reply_to_user_id`;

-- 4. 添加外键约束
ALTER TABLE `moment_comments` 
    ADD CONSTRAINT `fk_moment_comments_reply_user_id` FOREIGN KEY (`reply_to_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL; 