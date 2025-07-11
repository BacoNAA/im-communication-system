-- 简单清理login_devices表中的重复数据
-- 只保留基本的重复数据清理功能

-- 删除重复记录，保留ID最大的记录
DELETE ld1 FROM login_devices ld1
INNER JOIN login_devices ld2 
WHERE ld1.user_id = ld2.user_id 
  AND ld1.device_info = ld2.device_info 
  AND ld1.id < ld2.id;

-- 标准化device_type字段
UPDATE login_devices 
SET device_type = CASE 
    WHEN LOWER(device_type) = 'web' THEN 'Web'
    WHEN LOWER(device_type) = 'mobile' THEN 'Mobile'
    WHEN LOWER(device_type) = 'desktop' THEN 'Desktop'
    WHEN LOWER(device_type) = 'tablet' THEN 'Tablet'
    ELSE CONCAT(UPPER(SUBSTRING(device_type, 1, 1)), LOWER(SUBSTRING(device_type, 2)))
END
WHERE device_type IS NOT NULL;