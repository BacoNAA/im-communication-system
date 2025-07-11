-- 检查和修复device_type问题
-- 1. 首先查看当前device_type的分布情况
-- 2. 修复大小写问题
-- 3. 重新清理重复数据

-- 查看当前device_type分布（注释掉，仅供参考）
-- SELECT device_type, COUNT(*) as count FROM login_devices GROUP BY device_type ORDER BY device_type;

-- 步骤1: 修复device_type大小写问题
-- 将所有小写的device_type转换为首字母大写的格式
UPDATE login_devices 
SET device_type = CASE 
    WHEN LOWER(device_type) = 'web' THEN 'Web'
    WHEN LOWER(device_type) = 'mobile' THEN 'Mobile'
    WHEN LOWER(device_type) = 'tablet' THEN 'Tablet'
    WHEN LOWER(device_type) = 'desktop' THEN 'Desktop'
    WHEN LOWER(device_type) = 'android' THEN 'Android'
    WHEN LOWER(device_type) = 'ios' THEN 'iOS'
    WHEN LOWER(device_type) = 'windows' THEN 'Windows'
    WHEN LOWER(device_type) = 'mac' THEN 'Mac'
    WHEN LOWER(device_type) = 'linux' THEN 'Linux'
    ELSE device_type
END
WHERE LOWER(device_type) IN ('web', 'mobile', 'tablet', 'desktop', 'android', 'ios', 'windows', 'mac', 'linux')
  AND device_type != CASE 
    WHEN LOWER(device_type) = 'web' THEN 'Web'
    WHEN LOWER(device_type) = 'mobile' THEN 'Mobile'
    WHEN LOWER(device_type) = 'tablet' THEN 'Tablet'
    WHEN LOWER(device_type) = 'desktop' THEN 'Desktop'
    WHEN LOWER(device_type) = 'android' THEN 'Android'
    WHEN LOWER(device_type) = 'ios' THEN 'iOS'
    WHEN LOWER(device_type) = 'windows' THEN 'Windows'
    WHEN LOWER(device_type) = 'mac' THEN 'Mac'
    WHEN LOWER(device_type) = 'linux' THEN 'Linux'
    ELSE device_type
END;

-- 步骤2: 重新清理基于device_info的重复记录
-- 删除重复的设备记录，保留最新的记录（ID最大的）
DELETE ld1 FROM login_devices ld1
INNER JOIN login_devices ld2 
WHERE ld1.user_id = ld2.user_id 
  AND ld1.device_info = ld2.device_info 
  AND ld1.id < ld2.id;

-- 验证修复结果（注释掉，仅供参考）
-- SELECT device_type, COUNT(*) as count FROM login_devices GROUP BY device_type ORDER BY device_type;
-- SELECT user_id, device_info, COUNT(*) as count FROM login_devices GROUP BY user_id, device_info HAVING COUNT(*) > 1;