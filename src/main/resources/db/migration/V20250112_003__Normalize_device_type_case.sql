-- 统一login_devices表中device_type字段的大小写格式
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
WHERE device_type != CASE 
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