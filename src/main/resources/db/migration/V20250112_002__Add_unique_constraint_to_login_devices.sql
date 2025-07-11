-- 添加login_devices表的唯一约束，防止同一用户同一设备信息的重复记录
-- 同一用户可以在多个相同类型的设备上登录，但每个具体设备（device_info）应该是唯一的

-- 首先清理现有的重复数据，保留最新的记录
DELETE ld1 FROM login_devices ld1
INNER JOIN login_devices ld2 
WHERE ld1.user_id = ld2.user_id 
  AND ld1.device_info = ld2.device_info 
  AND ld1.id < ld2.id;

-- 添加唯一约束
ALTER TABLE login_devices 
ADD CONSTRAINT uk_login_devices_user_device_info 
UNIQUE (user_id, device_info);