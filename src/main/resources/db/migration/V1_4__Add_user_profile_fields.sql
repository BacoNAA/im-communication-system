-- 添加用户个人资料扩展字段
-- 版本: 1.4
-- 描述: 为users表添加手机号、性别、生日、所在地、职业等字段

ALTER TABLE users 
ADD COLUMN phone_number VARCHAR(20) COMMENT '手机号码',
ADD COLUMN gender VARCHAR(10) COMMENT '性别：男、女、保密',
ADD COLUMN birthday DATE COMMENT '生日',
ADD COLUMN location VARCHAR(100) COMMENT '所在地',
ADD COLUMN occupation VARCHAR(100) COMMENT '职业';

-- 添加索引以提高查询性能
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_gender ON users(gender);
CREATE INDEX idx_users_location ON users(location);

-- 添加约束确保性别字段的值正确
ALTER TABLE users 
ADD CONSTRAINT chk_gender CHECK (gender IN ('男', '女', '保密') OR gender IS NULL);