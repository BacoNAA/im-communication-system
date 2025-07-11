-- 清理基于 device_info 的重复设备记录
-- 保留每个 (user_id, device_info) 组合中最新的记录

-- 删除重复的设备记录，保留最新的记录
DELETE ld1 FROM login_devices ld1
INNER JOIN login_devices ld2 
WHERE ld1.user_id = ld2.user_id 
  AND ld1.device_info = ld2.device_info 
  AND ld1.id < ld2.id;

-- 验证清理结果
-- 以下查询应该返回0行，表示没有重复的 (user_id, device_info) 组合
-- SELECT user_id, device_info, COUNT(*) as count 
-- FROM login_devices 
-- GROUP BY user_id, device_info 
-- HAVING COUNT(*) > 1;