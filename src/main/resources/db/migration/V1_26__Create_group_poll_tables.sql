-- 群组投票主表
CREATE TABLE `polls` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投票唯一标识',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `title` VARCHAR(255) NOT NULL COMMENT '投票标题/问题',
    `description` VARCHAR(1000) NULL COMMENT '投票描述（可选）',
    `is_multiple` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否多选，TRUE为多选，FALSE为单选',
    `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否匿名投票',
    `status` ENUM('ACTIVE', 'ENDED', 'CANCELED') NOT NULL DEFAULT 'ACTIVE' COMMENT '投票状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `end_time` TIMESTAMP NULL COMMENT '结束时间（可选）',
    PRIMARY KEY (`id`),
    KEY `idx_polls_group_id` (`group_id`),
    KEY `idx_polls_creator_id` (`creator_id`),
    KEY `idx_polls_status` (`status`),
    KEY `idx_polls_created_at` (`created_at`),
    CONSTRAINT `fk_polls_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_polls_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组投票主表';

-- 投票选项表
CREATE TABLE `poll_options` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选项唯一标识',
    `poll_id` BIGINT NOT NULL COMMENT '关联投票ID',
    `option_text` VARCHAR(255) NOT NULL COMMENT '选项文本',
    `display_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    PRIMARY KEY (`id`),
    KEY `idx_poll_options_poll_id` (`poll_id`),
    CONSTRAINT `fk_poll_options_poll_id` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票选项表';

-- 用户投票记录表
CREATE TABLE `poll_votes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '投票记录唯一标识',
    `poll_id` BIGINT NOT NULL COMMENT '关联投票ID',
    `option_id` BIGINT NOT NULL COMMENT '选择的选项ID',
    `user_id` BIGINT NOT NULL COMMENT '投票用户ID',
    `voted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    PRIMARY KEY (`id`),
    KEY `idx_poll_votes_poll_id` (`poll_id`),
    KEY `idx_poll_votes_option_id` (`option_id`),
    KEY `idx_poll_votes_user_id` (`user_id`),
    UNIQUE KEY `uk_poll_votes_poll_option_user` (`poll_id`, `option_id`, `user_id`),
    CONSTRAINT `fk_poll_votes_poll_id` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_poll_votes_option_id` FOREIGN KEY (`option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_poll_votes_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户投票记录表'; 