-- 添加个人ID最后修改时间字段
ALTER TABLE users ADD COLUMN user_id_updated_at DATETIME NULL COMMENT '个人ID最后修改时间';

-- 为已有用户设置初始值（如果已设置个人ID，则设置为当前时间）
UPDATE users SET user_id_updated_at = NOW() WHERE user_id_str IS NOT NULL;