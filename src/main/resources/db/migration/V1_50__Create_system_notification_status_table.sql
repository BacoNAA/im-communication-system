-- 创建系统通知用户状态表
CREATE TABLE `system_notification_status` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `notification_id` BIGINT NOT NULL COMMENT '系统通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    `read_at` TIMESTAMP NULL COMMENT '阅读时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_user` (`notification_id`, `user_id`),
    KEY `idx_notification_status_user_id` (`user_id`),
    KEY `idx_notification_status_is_read` (`is_read`),
    KEY `idx_notification_status_read_at` (`read_at`),
    CONSTRAINT `fk_notification_status_notification_id` FOREIGN KEY (`notification_id`) REFERENCES `system_notifications` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_status_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知用户状态表'; 