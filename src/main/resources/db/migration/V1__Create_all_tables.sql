-- IM通信系统数据库初始化脚本
-- 根据需求分析和系统详细设计书 2.3节 创建所有数据表
-- 版本: V1.0
-- 创建时间: 2024-01-01

-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 用户相关表
-- ========================================

-- 用户表
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识，主键',
    `email` VARCHAR(255) NOT NULL COMMENT '注册邮箱，用于登录',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '加盐哈希后的密码',
    `nickname` VARCHAR(50) NOT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(255) NULL COMMENT '头像图片的URL',
    `signature` VARCHAR(255) NULL COMMENT '个性签名',
    `user_id_str` VARCHAR(50) NULL COMMENT '用户设置的唯一ID，可用于搜索',
    `status_json` JSON NULL COMMENT '个性化状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_user_id_str` (`user_id_str`),
    KEY `idx_users_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户资料表
CREATE TABLE `user_profiles` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID，主键',
    `real_name` VARCHAR(50) NULL COMMENT '真实姓名',
    `gender` ENUM('male', 'female', 'unknown') NOT NULL DEFAULT 'unknown' COMMENT '性别',
    `birthday` DATE NULL COMMENT '生日',
    `location` VARCHAR(100) NULL COMMENT '所在地',
    `bio` TEXT NULL COMMENT '个人简介',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料表';

-- 用户设置表
CREATE TABLE `user_settings` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID，主键',
    `privacy_settings` JSON NULL COMMENT '隐私设置',
    `notification_settings` JSON NULL COMMENT '通知设置',
    `theme_settings` JSON NULL COMMENT '主题设置',
    `language` VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT '语言设置',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_settings_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置表';

-- 登录设备表
CREATE TABLE `login_devices` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '关联的用户ID',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型',
    `device_info` VARCHAR(255) NULL COMMENT '设备信息',
    `ip_address` VARCHAR(45) NULL COMMENT '登录IP地址',
    `last_login_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次登录时间',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '会话是否依然有效',
    PRIMARY KEY (`id`),
    KEY `idx_login_devices_user_id` (`user_id`),
    KEY `idx_login_devices_last_login_at` (`last_login_at`),
    CONSTRAINT `fk_login_devices_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备表';

-- ========================================
-- 2. 联系人相关表
-- ========================================

-- 好友关系表
CREATE TABLE `contacts` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友的用户ID',
    `alias` VARCHAR(50) NULL COMMENT '用户为好友设置的备注名',
    `is_blocked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否拉黑了好友',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立好友关系的时间',
    PRIMARY KEY (`user_id`, `friend_id`),
    KEY `idx_contacts_friend_id` (`friend_id`),
    KEY `idx_contacts_created_at` (`created_at`),
    CONSTRAINT `fk_contacts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_contacts_friend_id` FOREIGN KEY (`friend_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 好友请求表
CREATE TABLE `friend_requests` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '请求唯一标识',
    `requester_id` BIGINT NOT NULL COMMENT '请求方ID',
    `recipient_id` BIGINT NOT NULL COMMENT '接收方ID',
    `message` VARCHAR(255) NULL COMMENT '验证信息',
    `status` ENUM('pending', 'accepted', 'rejected') NOT NULL DEFAULT 'pending' COMMENT '请求状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求发送时间',
    `handled_at` TIMESTAMP NULL COMMENT '请求处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_friend_requests_requester_id` (`requester_id`),
    KEY `idx_friend_requests_recipient_id` (`recipient_id`),
    KEY `idx_friend_requests_status` (`status`),
    KEY `idx_friend_requests_created_at` (`created_at`),
    CONSTRAINT `fk_friend_requests_requester_id` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_friend_requests_recipient_id` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友请求表';

-- 联系人标签表
CREATE TABLE `contact_tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '标签所有者ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `color` VARCHAR(7) NULL COMMENT '标签颜色',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_contact_tags_user_id` (`user_id`),
    CONSTRAINT `fk_contact_tags_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人标签表';

-- 屏蔽列表表
CREATE TABLE `block_lists` (
    `user_id` BIGINT NOT NULL COMMENT '屏蔽者ID',
    `blocked_user_id` BIGINT NOT NULL COMMENT '被屏蔽者ID',
    `reason` VARCHAR(100) NULL COMMENT '屏蔽原因',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '屏蔽时间',
    PRIMARY KEY (`user_id`, `blocked_user_id`),
    KEY `idx_block_lists_blocked_user_id` (`blocked_user_id`),
    KEY `idx_block_lists_created_at` (`created_at`),
    CONSTRAINT `fk_block_lists_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_block_lists_blocked_user_id` FOREIGN KEY (`blocked_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='屏蔽列表表';

-- ========================================
-- 3. 消息相关表
-- ========================================

-- 会话表
CREATE TABLE `conversations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话唯一标识',
    `type` ENUM('private', 'group') NOT NULL COMMENT '会话类型：private为私聊, group为群聊',
    `related_group_id` BIGINT NULL COMMENT '如果是群聊，关联的群组ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversations_type` (`type`),
    KEY `idx_conversations_related_group_id` (`related_group_id`),
    KEY `idx_conversations_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 会话成员表
CREATE TABLE `conversation_members` (
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `is_pinned` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否置顶此会话',
    `is_archived` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否归档此会话',
    `is_dnd` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为此会话开启免打扰',
    `draft` TEXT NULL COMMENT '在此会话中未发送的草稿',
    `last_read_message_id` BIGINT NULL COMMENT '最后已读消息的ID',
    PRIMARY KEY (`conversation_id`, `user_id`),
    KEY `idx_conversation_members_user_id` (`user_id`),
    KEY `idx_conversation_members_is_pinned` (`is_pinned`),
    CONSTRAINT `fk_conversation_members_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_conversation_members_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员表';

-- 消息表
CREATE TABLE `messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息唯一标识',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送方用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '消息类型',
    `content` JSON NOT NULL COMMENT '消息内容',
    `parent_message_id` BIGINT NULL COMMENT '父消息ID，用于实现话题串',
    `is_recalled` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已撤回',
    `edited_at` TIMESTAMP NULL COMMENT '消息最后编辑时间',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间，精确到毫秒',
    PRIMARY KEY (`id`),
    KEY `idx_messages_conversation_id` (`conversation_id`),
    KEY `idx_messages_sender_id` (`sender_id`),
    KEY `idx_messages_type` (`type`),
    KEY `idx_messages_parent_message_id` (`parent_message_id`),
    KEY `idx_messages_created_at` (`created_at`),
    CONSTRAINT `fk_messages_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_messages_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_messages_parent_message_id` FOREIGN KEY (`parent_message_id`) REFERENCES `messages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ========================================
-- 4. 群组相关表
-- ========================================

-- 群组表
CREATE TABLE `groups` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `owner_id` BIGINT NOT NULL COMMENT '群主的用户ID',
    `avatar_url` VARCHAR(255) NULL COMMENT '群头像URL',
    `description` TEXT NULL COMMENT '群介绍',
    `requires_approval` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '新成员加入是否需要审批',
    `is_all_muted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否开启全体禁言',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_groups_owner_id` (`owner_id`),
    KEY `idx_groups_created_at` (`created_at`),
    CONSTRAINT `fk_groups_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群成员表
CREATE TABLE `group_members` (
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` ENUM('owner', 'admin', 'member') NOT NULL DEFAULT 'member' COMMENT '成员在群中的角色',
    `is_muted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否被单独禁言',
    `muted_until` TIMESTAMP NULL COMMENT '禁言截止时间',
    `joined_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入群组的时间',
    PRIMARY KEY (`group_id`, `user_id`),
    KEY `idx_group_members_user_id` (`user_id`),
    KEY `idx_group_members_role` (`role`),
    KEY `idx_group_members_joined_at` (`joined_at`),
    CONSTRAINT `fk_group_members_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_members_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 群组邀请表
CREATE TABLE `group_invites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请唯一标识',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `inviter_id` BIGINT NOT NULL COMMENT '邀请者ID',
    `invitee_id` BIGINT NOT NULL COMMENT '被邀请者ID',
    `message` VARCHAR(255) NULL COMMENT '邀请消息',
    `status` ENUM('pending', 'accepted', 'rejected', 'expired') NOT NULL DEFAULT 'pending' COMMENT '邀请状态',
    `expires_at` TIMESTAMP NULL COMMENT '邀请过期时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间',
    `handled_at` TIMESTAMP NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_invites_group_id` (`group_id`),
    KEY `idx_group_invites_inviter_id` (`inviter_id`),
    KEY `idx_group_invites_invitee_id` (`invitee_id`),
    KEY `idx_group_invites_status` (`status`),
    KEY `idx_group_invites_created_at` (`created_at`),
    CONSTRAINT `fk_group_invites_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_invites_inviter_id` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_invites_invitee_id` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组邀请表';

-- 群公告表
CREATE TABLE `group_announcements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告唯一标识',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `author_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `is_pinned` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否置顶',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_announcements_group_id` (`group_id`),
    KEY `idx_group_announcements_author_id` (`author_id`),
    KEY `idx_group_announcements_is_pinned` (`is_pinned`),
    KEY `idx_group_announcements_created_at` (`created_at`),
    CONSTRAINT `fk_group_announcements_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_announcements_author_id` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群公告表';

-- ========================================
-- 5. 动态相关表
-- ========================================

-- 动态表
CREATE TABLE `moments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '动态唯一标识',
    `author_id` BIGINT NOT NULL COMMENT '发布者用户ID',
    `content` TEXT NULL COMMENT '文字内容',
    `media_json` JSON NULL COMMENT '媒体文件信息',
    `visibility_json` JSON NOT NULL COMMENT '可见性规则',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_moments_author_id` (`author_id`),
    KEY `idx_moments_created_at` (`created_at`),
    CONSTRAINT `fk_moments_author_id` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态表';

-- 动态点赞表
CREATE TABLE `moment_likes` (
    `moment_id` BIGINT NOT NULL COMMENT '动态ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`moment_id`, `user_id`),
    KEY `idx_moment_likes_user_id` (`user_id`),
    KEY `idx_moment_likes_created_at` (`created_at`),
    CONSTRAINT `fk_moment_likes_moment_id` FOREIGN KEY (`moment_id`) REFERENCES `moments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_moment_likes_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态点赞表';

-- 动态评论表
CREATE TABLE `moment_comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论唯一标识',
    `moment_id` BIGINT NOT NULL COMMENT '所属动态ID',
    `author_id` BIGINT NOT NULL COMMENT '评论者用户ID',
    `parent_comment_id` BIGINT NULL COMMENT '回复的父评论ID',
    `content` TEXT NOT NULL COMMENT '评论文字内容',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_moment_comments_moment_id` (`moment_id`),
    KEY `idx_moment_comments_author_id` (`author_id`),
    KEY `idx_moment_comments_parent_comment_id` (`parent_comment_id`),
    KEY `idx_moment_comments_created_at` (`created_at`),
    CONSTRAINT `fk_moment_comments_moment_id` FOREIGN KEY (`moment_id`) REFERENCES `moments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_moment_comments_author_id` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_moment_comments_parent_comment_id` FOREIGN KEY (`parent_comment_id`) REFERENCES `moment_comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态评论表';

-- 动态媒体表
CREATE TABLE `moment_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '媒体唯一标识',
    `moment_id` BIGINT NOT NULL COMMENT '所属动态ID',
    `media_type` ENUM('image', 'video') NOT NULL COMMENT '媒体类型',
    `media_url` VARCHAR(255) NOT NULL COMMENT '媒体文件URL',
    `thumbnail_url` VARCHAR(255) NULL COMMENT '缩略图URL',
    `file_size` BIGINT NULL COMMENT '文件大小（字节）',
    `duration` INT NULL COMMENT '视频时长（秒）',
    `width` INT NULL COMMENT '媒体宽度',
    `height` INT NULL COMMENT '媒体高度',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_moment_media_moment_id` (`moment_id`),
    KEY `idx_moment_media_media_type` (`media_type`),
    KEY `idx_moment_media_created_at` (`created_at`),
    CONSTRAINT `fk_moment_media_moment_id` FOREIGN KEY (`moment_id`) REFERENCES `moments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态媒体表';

-- ========================================
-- 6. 通话相关表
-- ========================================

-- 通话记录表
CREATE TABLE `calls` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通话唯一标识',
    `conversation_id` BIGINT NOT NULL COMMENT '关联的会话ID',
    `initiator_id` BIGINT NOT NULL COMMENT '发起通话的用户ID',
    `type` ENUM('audio', 'video') NOT NULL COMMENT '通话类型：音频或视频',
    `status` ENUM('calling', 'connected', 'ended', 'cancelled', 'rejected') NOT NULL DEFAULT 'calling' COMMENT '通话状态',
    `started_at` TIMESTAMP NULL COMMENT '通话开始时间',
    `ended_at` TIMESTAMP NULL COMMENT '通话结束时间',
    `duration` INT NULL COMMENT '通话时长（秒）',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通话发起时间',
    PRIMARY KEY (`id`),
    KEY `idx_calls_conversation_id` (`conversation_id`),
    KEY `idx_calls_initiator_id` (`initiator_id`),
    KEY `idx_calls_type` (`type`),
    KEY `idx_calls_status` (`status`),
    KEY `idx_calls_created_at` (`created_at`),
    CONSTRAINT `fk_calls_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_calls_initiator_id` FOREIGN KEY (`initiator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通话记录表';

-- 通话参与者表
CREATE TABLE `call_participants` (
    `call_id` BIGINT NOT NULL COMMENT '通话ID',
    `user_id` BIGINT NOT NULL COMMENT '参与者用户ID',
    `joined_at` TIMESTAMP NULL COMMENT '加入通话时间',
    `left_at` TIMESTAMP NULL COMMENT '离开通话时间',
    `is_muted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否静音',
    `is_video_enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否开启视频',
    PRIMARY KEY (`call_id`, `user_id`),
    KEY `idx_call_participants_user_id` (`user_id`),
    CONSTRAINT `fk_call_participants_call_id` FOREIGN KEY (`call_id`) REFERENCES `calls` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_call_participants_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通话参与者表';

-- 通话会话表
CREATE TABLE `call_sessions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话唯一标识',
    `call_id` BIGINT NOT NULL COMMENT '通话ID',
    `session_id` VARCHAR(100) NOT NULL COMMENT 'WebRTC会话ID',
    `peer_connection_id` VARCHAR(100) NULL COMMENT '对等连接ID',
    `ice_candidates` JSON NULL COMMENT 'ICE候选信息',
    `sdp_offer` TEXT NULL COMMENT 'SDP提议',
    `sdp_answer` TEXT NULL COMMENT 'SDP应答',
    `connection_state` VARCHAR(20) NOT NULL DEFAULT 'new' COMMENT '连接状态',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_call_sessions_session_id` (`session_id`),
    KEY `idx_call_sessions_call_id` (`call_id`),
    KEY `idx_call_sessions_connection_state` (`connection_state`),
    CONSTRAINT `fk_call_sessions_call_id` FOREIGN KEY (`call_id`) REFERENCES `calls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通话会话表';

-- 通话记录表
CREATE TABLE `call_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录唯一标识',
    `call_id` BIGINT NOT NULL COMMENT '通话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `action_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `action_data` JSON NULL COMMENT '操作数据',
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_call_records_call_id` (`call_id`),
    KEY `idx_call_records_user_id` (`user_id`),
    KEY `idx_call_records_action_type` (`action_type`),
    KEY `idx_call_records_timestamp` (`timestamp`),
    CONSTRAINT `fk_call_records_call_id` FOREIGN KEY (`call_id`) REFERENCES `calls` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_call_records_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通话记录表';

-- ========================================
-- 7. 管理相关表
-- ========================================

-- 管理员表
CREATE TABLE `admin_users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '加盐哈希后的密码',
    `email` VARCHAR(255) NOT NULL COMMENT '管理员邮箱',
    `role` ENUM('super_admin', 'admin', 'moderator') NOT NULL DEFAULT 'moderator' COMMENT '管理员角色',
    `permissions` JSON NULL COMMENT '权限配置',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_login_at` TIMESTAMP NULL COMMENT '最后登录时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_users_username` (`username`),
    UNIQUE KEY `uk_admin_users_email` (`email`),
    KEY `idx_admin_users_role` (`role`),
    KEY `idx_admin_users_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 管理操作日志表
CREATE TABLE `admin_operation_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志唯一标识',
    `admin_id` BIGINT NOT NULL COMMENT '操作管理员ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(50) NOT NULL COMMENT '操作目标类型',
    `target_id` BIGINT NULL COMMENT '操作目标ID',
    `description` TEXT NULL COMMENT '操作描述',
    `ip_address` VARCHAR(45) NULL COMMENT '操作IP地址',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_operation_logs_admin_id` (`admin_id`),
    KEY `idx_admin_operation_logs_operation_type` (`operation_type`),
    KEY `idx_admin_operation_logs_target_type` (`target_type`),
    KEY `idx_admin_operation_logs_created_at` (`created_at`),
    CONSTRAINT `fk_admin_operation_logs_admin_id` FOREIGN KEY (`admin_id`) REFERENCES `admin_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理操作日志表';

-- 举报表
CREATE TABLE `reports` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报唯一标识',
    `reporter_id` BIGINT NOT NULL COMMENT '举报者用户ID',
    `reported_user_id` BIGINT NULL COMMENT '被举报用户ID',
    `reported_content_type` VARCHAR(50) NOT NULL COMMENT '被举报内容类型',
    `reported_content_id` BIGINT NULL COMMENT '被举报内容ID',
    `reason` VARCHAR(100) NOT NULL COMMENT '举报原因',
    `description` TEXT NULL COMMENT '详细描述',
    `status` ENUM('pending', 'processing', 'resolved', 'rejected') NOT NULL DEFAULT 'pending' COMMENT '处理状态',
    `handler_id` BIGINT NULL COMMENT '处理管理员ID',
    `handled_at` TIMESTAMP NULL COMMENT '处理时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`),
    KEY `idx_reports_reporter_id` (`reporter_id`),
    KEY `idx_reports_reported_user_id` (`reported_user_id`),
    KEY `idx_reports_reported_content_type` (`reported_content_type`),
    KEY `idx_reports_status` (`status`),
    KEY `idx_reports_handler_id` (`handler_id`),
    KEY `idx_reports_created_at` (`created_at`),
    CONSTRAINT `fk_reports_reporter_id` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reports_reported_user_id` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_reports_handler_id` FOREIGN KEY (`handler_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- 内容审核表
CREATE TABLE `content_moderations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核唯一标识',
    `content_type` VARCHAR(50) NOT NULL COMMENT '内容类型',
    `content_id` BIGINT NOT NULL COMMENT '内容ID',
    `reporter_id` BIGINT NULL COMMENT '举报者ID',
    `moderator_id` BIGINT NULL COMMENT '审核员ID',
    `status` ENUM('pending', 'approved', 'rejected', 'deleted') NOT NULL DEFAULT 'pending' COMMENT '审核状态',
    `reason` VARCHAR(255) NULL COMMENT '审核原因',
    `action_taken` VARCHAR(100) NULL COMMENT '采取的行动',
    `notes` TEXT NULL COMMENT '审核备注',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handled_at` TIMESTAMP NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_content_moderations_content_type` (`content_type`),
    KEY `idx_content_moderations_content_id` (`content_id`),
    KEY `idx_content_moderations_reporter_id` (`reporter_id`),
    KEY `idx_content_moderations_moderator_id` (`moderator_id`),
    KEY `idx_content_moderations_status` (`status`),
    KEY `idx_content_moderations_created_at` (`created_at`),
    CONSTRAINT `fk_content_moderations_reporter_id` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_content_moderations_moderator_id` FOREIGN KEY (`moderator_id`) REFERENCES `admin_users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核表';

-- 系统通知表
CREATE TABLE `system_notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知唯一标识',
    `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
    `target_type` ENUM('all', 'specific_users') NOT NULL DEFAULT 'all' COMMENT '目标类型',
    `target_users` JSON NULL COMMENT '目标用户ID列表',
    `is_published` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已发布',
    `published_at` TIMESTAMP NULL COMMENT '发布时间',
    `created_by` BIGINT NOT NULL COMMENT '创建者管理员ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_system_notifications_type` (`type`),
    KEY `idx_system_notifications_target_type` (`target_type`),
    KEY `idx_system_notifications_is_published` (`is_published`),
    KEY `idx_system_notifications_created_by` (`created_by`),
    KEY `idx_system_notifications_created_at` (`created_at`),
    CONSTRAINT `fk_system_notifications_created_by` FOREIGN KEY (`created_by`) REFERENCES `admin_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ========================================
-- 添加外键约束（群组相关）
-- ========================================

-- 为conversations表添加groups外键约束
ALTER TABLE `conversations` ADD CONSTRAINT `fk_conversations_related_group_id` FOREIGN KEY (`related_group_id`) REFERENCES `groups` (`id`) ON DELETE SET NULL;

-- ========================================
-- 创建验证码表（已存在但需要确保一致性）
-- ========================================

-- 验证码表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS `verification_codes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱地址',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` ENUM('register', 'login', 'reset_password') NOT NULL COMMENT '验证码类型',
    `expires_at` TIMESTAMP NOT NULL COMMENT '过期时间',
    `is_used` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已使用',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_verification_codes_email` (`email`),
    KEY `idx_verification_codes_code` (`code`),
    KEY `idx_verification_codes_type` (`type`),
    KEY `idx_verification_codes_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 插入初始数据（可选）
-- 这里可以添加一些初始的管理员账户或系统配置数据

-- 创建默认超级管理员（密码: admin123，请在生产环境中修改）
INSERT INTO `admin_users` (`username`, `password_hash`, `email`, `role`, `is_active`) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXIGfR8f5.7JxGLZJpz8qF.Qp/y', 'admin@im-system.com', 'super_admin', TRUE)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- 数据库初始化完成
-- 总计创建了21个核心表，符合需求分析和系统详细设计书2.3节的要求
-- 包括：用户相关表(4个)、联系人相关表(4个)、消息相关表(3个)、群组相关表(4个)、动态相关表(4个)、通话相关表(4个)、管理相关表(4个)
-- 所有表都使用InnoDB存储引擎，支持事务和外键约束
-- 字符集为utf8mb4，支持Emoji表情和多语言
-- 已添加适当的索引以优化查询性能
-- 外键约束确保数据完整性