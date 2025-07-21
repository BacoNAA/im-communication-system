-- 群组加入请求表
CREATE TABLE `group_join_requests` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '请求唯一标识',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '申请者ID',
    `message` VARCHAR(255) NULL COMMENT '申请消息',
    `status` ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `handled_at` TIMESTAMP NULL COMMENT '处理时间',
    `handler_id` BIGINT NULL COMMENT '处理者ID',
    PRIMARY KEY (`id`),
    KEY `idx_group_join_requests_group_id` (`group_id`),
    KEY `idx_group_join_requests_user_id` (`user_id`),
    KEY `idx_group_join_requests_status` (`status`),
    KEY `idx_group_join_requests_created_at` (`created_at`),
    CONSTRAINT `fk_group_join_requests_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_join_requests_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组加入请求表'; 