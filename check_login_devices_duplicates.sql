-- 检查login_devices表中的重复数据问题
-- 执行此脚本来诊断和修复重复设备记录问题

-- 1. 检查是否存在重复的 (user_id, device_info) 组合
SELECT 
    user_id, 
    device_info, 
    COUNT(*) as duplicate_count,
    GROUP_CONCAT(id ORDER BY id) as device_ids,
    GROUP_CONCAT(device_type ORDER BY id) as device_types,
    GROUP_CONCAT(is_active ORDER BY id) as active_status
FROM login_devices 
GROUP BY user_id, device_info 
HAVING COUNT(*) > 1
ORDER BY user_id, device_info;

-- 2. 检查device_type的大小写分布
SELECT 
    device_type, 
    COUNT(*) as count 
FROM login_devices 
GROUP BY device_type 
ORDER BY device_type;

-- 3. 检查唯一约束是否存在
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_NAME = 'login_devices' 
    AND CONSTRAINT_TYPE = 'UNIQUE';

-- 4. 如果发现重复数据，执行以下清理脚本：
-- （注释掉，需要时手动执行）

/*
-- 清理重复数据，保留最新的记录
DELETE ld1 FROM login_devices ld1
INNER JOIN login_devices ld2 
WHERE ld1.user_id = ld2.user_id 
  AND ld1.device_info = ld2.device_info 
  AND ld1.id < ld2.id;

-- 如果唯一约束不存在，添加唯一约束
ALTER TABLE login_devices 
ADD CONSTRAINT uk_login_devices_user_device_info 
UNIQUE (user_id, device_info);
*/

-- 5. 验证修复结果
-- 执行完清理后，再次运行查询1，应该返回0行结果