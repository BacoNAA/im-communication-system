package com.im.imcommunicationsystem.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类
 * 提供Redis缓存操作的统一接口
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class CacheUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认过期时间（秒）
     */
    public static final long DEFAULT_EXPIRE = 3600; // 1小时

    /**
     * 短期过期时间（秒）
     */
    public static final long SHORT_EXPIRE = 300; // 5分钟

    /**
     * 长期过期时间（秒）
     */
    public static final long LONG_EXPIRE = 86400; // 24小时

    /**
     * 分布式锁默认过期时间（秒）
     */
    public static final long LOCK_EXPIRE = 30;

    /**
     * 缓存键前缀
     */
    public static final String KEY_PREFIX = "im:";

    // ==================== 基础操作 ====================

    /**
     * 设置缓存
     * 
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 设置缓存（带过期时间）
     * 
     * @param key 键
     * @param value 值
     * @param expire 过期时间（秒）
     */
    public void set(String key, Object value, long expire) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForValue().set(fullKey, value, expire, TimeUnit.SECONDS);
            log.debug("设置缓存成功: key={}, expire={}", fullKey, expire);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
        }
    }

    /**
     * 获取缓存
     * 
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().get(fullKey);
        } catch (Exception e) {
            log.error("获取缓存失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存（指定类型）
     * 
     * @param key 键
     * @param clazz 值类型
     * @param <T> 泛型类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        
        try {
            if (clazz.isInstance(value)) {
                return (T) value;
            }
            
            // 如果是字符串类型，尝试JSON反序列化
            if (value instanceof String && !String.class.equals(clazz)) {
                return JsonUtils.fromJson((String) value, clazz);
            }
            
            return (T) value;
        } catch (Exception e) {
            log.error("缓存类型转换失败: key={}, targetType={}", key, clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 获取缓存，如果不存在则执行supplier并缓存结果
     * 
     * @param key 键
     * @param supplier 数据提供者
     * @param expire 过期时间（秒）
     * @param <T> 泛型类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrSet(String key, Supplier<T> supplier, long expire) {
        Object value = get(key);
        if (value != null) {
            return (T) value;
        }
        
        try {
            T result = supplier.get();
            if (result != null) {
                set(key, result, expire);
            }
            return result;
        } catch (Exception e) {
            log.error("缓存穿透处理失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     * 
     * @param key 键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.delete(fullKey);
            log.debug("删除缓存: key={}, result={}", fullKey, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     * 
     * @param keys 键列表
     * @return 删除的数量
     */
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        try {
            List<String> fullKeys = new ArrayList<>();
            for (String key : keys) {
                fullKeys.add(buildKey(key));
            }
            
            Long result = redisTemplate.delete(fullKeys);
            log.debug("批量删除缓存: keys={}, result={}", fullKeys, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("批量删除缓存失败: keys={}", keys, e);
            return 0;
        }
    }

    /**
     * 检查键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.hasKey(fullKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     * 
     * @param key 键
     * @param expire 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean expire(String key, long expire) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.expire(fullKey, expire, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置过期时间失败: key={}, expire={}", key, expire, e);
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     * 
     * @param key 键
     * @return 剩余时间（秒），-1表示永不过期，-2表示键不存在
     */
    public long getExpire(String key) {
        try {
            String fullKey = buildKey(key);
            Long expire = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
            return expire != null ? expire : -2;
        } catch (Exception e) {
            log.error("获取过期时间失败: key={}", key, e);
            return -2;
        }
    }

    // ==================== 计数器操作 ====================

    /**
     * 递增
     * 
     * @param key 键
     * @return 递增后的值
     */
    public long increment(String key) {
        return increment(key, 1);
    }

    /**
     * 递增指定值
     * 
     * @param key 键
     * @param delta 增量
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForValue().increment(fullKey, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递增操作失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }

    /**
     * 递减
     * 
     * @param key 键
     * @return 递减后的值
     */
    public long decrement(String key) {
        return decrement(key, 1);
    }

    /**
     * 递减指定值
     * 
     * @param key 键
     * @param delta 减量
     * @return 递减后的值
     */
    public long decrement(String key, long delta) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForValue().decrement(fullKey, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递减操作失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }

    // ==================== Hash操作 ====================

    /**
     * 设置Hash字段
     * 
     * @param key 键
     * @param field 字段
     * @param value 值
     */
    public void hSet(String key, String field, Object value) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForHash().put(fullKey, field, value);
        } catch (Exception e) {
            log.error("设置Hash字段失败: key={}, field={}", key, field, e);
        }
    }

    /**
     * 获取Hash字段值
     * 
     * @param key 键
     * @param field 字段
     * @return 值
     */
    public Object hGet(String key, String field) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().get(fullKey, field);
        } catch (Exception e) {
            log.error("获取Hash字段失败: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * 获取Hash所有字段
     * 
     * @param key 键
     * @return 字段Map
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().entries(fullKey);
        } catch (Exception e) {
            log.error("获取Hash所有字段失败: key={}", key, e);
            return new HashMap<>();
        }
    }

    /**
     * 删除Hash字段
     * 
     * @param key 键
     * @param fields 字段列表
     * @return 删除的字段数量
     */
    public long hDelete(String key, Object... fields) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForHash().delete(fullKey, fields);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("删除Hash字段失败: key={}, fields={}", key, Arrays.toString(fields), e);
            return 0;
        }
    }

    // ==================== Set操作 ====================

    /**
     * 添加Set元素
     * 
     * @param key 键
     * @param values 值列表
     * @return 添加的元素数量
     */
    public long sAdd(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForSet().add(fullKey, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("添加Set元素失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 获取Set所有元素
     * 
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        try {
            String fullKey = buildKey(key);
            Set<Object> result = redisTemplate.opsForSet().members(fullKey);
            return result != null ? result : new HashSet<>();
        } catch (Exception e) {
            log.error("获取Set元素失败: key={}", key, e);
            return new HashSet<>();
        }
    }

    /**
     * 检查Set是否包含元素
     * 
     * @param key 键
     * @param value 值
     * @return 是否包含
     */
    public boolean sIsMember(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.opsForSet().isMember(fullKey, value);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("检查Set元素失败: key={}, value={}", key, value, e);
            return false;
        }
    }

    /**
     * 移除Set元素
     * 
     * @param key 键
     * @param values 值列表
     * @return 移除的元素数量
     */
    public long sRemove(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForSet().remove(fullKey, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("移除Set元素失败: key={}", key, e);
            return 0;
        }
    }

    // ==================== List操作 ====================

    /**
     * 从左侧推入List元素
     * 
     * @param key 键
     * @param values 值列表
     * @return List长度
     */
    public long lPush(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForList().leftPushAll(fullKey, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("左侧推入List元素失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 从右侧推入List元素
     * 
     * @param key 键
     * @param values 值列表
     * @return List长度
     */
    public long rPush(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.opsForList().rightPushAll(fullKey, values);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("右侧推入List元素失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 从左侧弹出List元素
     * 
     * @param key 键
     * @return 弹出的元素
     */
    public Object lPop(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().leftPop(fullKey);
        } catch (Exception e) {
            log.error("左侧弹出List元素失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 从右侧弹出List元素
     * 
     * @param key 键
     * @return 弹出的元素
     */
    public Object rPop(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().rightPop(fullKey);
        } catch (Exception e) {
            log.error("右侧弹出List元素失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取List范围内的元素
     * 
     * @param key 键
     * @param start 开始索引
     * @param end 结束索引
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            List<Object> result = redisTemplate.opsForList().range(fullKey, start, end);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取List范围元素失败: key={}, start={}, end={}", key, start, end, e);
            return new ArrayList<>();
        }
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁键
     * @param requestId 请求ID（用于释放锁时验证）
     * @param expire 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expire) {
        try {
            String fullKey = buildKey("lock:" + lockKey);
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, requestId, expire, TimeUnit.SECONDS);
            boolean success = Boolean.TRUE.equals(result);
            log.debug("尝试获取分布式锁: key={}, requestId={}, success={}", fullKey, requestId, success);
            return success;
        } catch (Exception e) {
            log.error("获取分布式锁失败: lockKey={}, requestId={}", lockKey, requestId, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * 
     * @param lockKey 锁键
     * @param requestId 请求ID
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String requestId) {
        try {
            String fullKey = buildKey("lock:" + lockKey);
            
            // Lua脚本确保原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('del', KEYS[1]) " +
                           "else return 0 end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(fullKey), requestId);
            boolean success = Long.valueOf(1).equals(result);
            log.debug("释放分布式锁: key={}, requestId={}, success={}", fullKey, requestId, success);
            return success;
        } catch (Exception e) {
            log.error("释放分布式锁失败: lockKey={}, requestId={}", lockKey, requestId, e);
            return false;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建完整的缓存键
     * 
     * @param key 原始键
     * @return 完整键
     */
    private String buildKey(String key) {
        if (StringUtils.hasText(key) && key.startsWith(KEY_PREFIX)) {
            return key;
        }
        return KEY_PREFIX + key;
    }

    /**
     * 批量删除匹配模式的键
     * 
     * @param pattern 匹配模式
     * @return 删除的数量
     */
    public long deleteByPattern(String pattern) {
        try {
            String fullPattern = buildKey(pattern);
            Set<String> keys = redisTemplate.keys(fullPattern);
            if (keys != null && !keys.isEmpty()) {
                Long result = redisTemplate.delete(keys);
                log.debug("按模式删除缓存: pattern={}, count={}", fullPattern, result);
                return result != null ? result : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("按模式删除缓存失败: pattern={}", pattern, e);
            return 0;
        }
    }

    /**
     * 获取匹配模式的所有键
     * 
     * @param pattern 匹配模式
     * @return 键集合
     */
    public Set<String> getKeysByPattern(String pattern) {
        try {
            String fullPattern = buildKey(pattern);
            Set<String> keys = redisTemplate.keys(fullPattern);
            return keys != null ? keys : new HashSet<>();
        } catch (Exception e) {
            log.error("获取匹配键失败: pattern={}", pattern, e);
            return new HashSet<>();
        }
    }

    /**
     * 缓存工具使用说明：
     * 
     * 1. 基础操作：
     *    - set() - 设置缓存
     *    - get() - 获取缓存
     *    - getOrSet() - 获取或设置缓存（防止缓存穿透）
     *    - delete() - 删除缓存
     *    - exists() - 检查键是否存在
     *    - expire() - 设置过期时间
     * 
     * 2. 计数器操作：
     *    - increment() - 递增
     *    - decrement() - 递减
     * 
     * 3. Hash操作：
     *    - hSet() - 设置Hash字段
     *    - hGet() - 获取Hash字段
     *    - hGetAll() - 获取所有Hash字段
     *    - hDelete() - 删除Hash字段
     * 
     * 4. Set操作：
     *    - sAdd() - 添加Set元素
     *    - sMembers() - 获取所有Set元素
     *    - sIsMember() - 检查Set是否包含元素
     *    - sRemove() - 移除Set元素
     * 
     * 5. List操作：
     *    - lPush() / rPush() - 推入List元素
     *    - lPop() / rPop() - 弹出List元素
     *    - lRange() - 获取List范围元素
     * 
     * 6. 分布式锁：
     *    - tryLock() - 尝试获取锁
     *    - releaseLock() - 释放锁
     * 
     * 使用示例：
     * 
     * // 基础缓存操作
     * cacheUtils.set("user:123", user, 3600);
     * User user = cacheUtils.get("user:123", User.class);
     * 
     * // 防止缓存穿透
     * User user = cacheUtils.getOrSet("user:123", () -> {
     *     return userService.findById(123);
     * }, 3600);
     * 
     * // 计数器
     * long count = cacheUtils.increment("login:count:" + userId);
     * 
     * // Hash操作（用户会话）
     * cacheUtils.hSet("session:" + sessionId, "userId", userId);
     * cacheUtils.hSet("session:" + sessionId, "loginTime", System.currentTimeMillis());
     * 
     * // Set操作（在线用户）
     * cacheUtils.sAdd("online:users", userId);
     * boolean isOnline = cacheUtils.sIsMember("online:users", userId);
     * 
     * // 分布式锁
     * String requestId = UUID.randomUUID().toString();
     * if (cacheUtils.tryLock("order:" + orderId, requestId, 30)) {
     *     try {
     *         // 执行业务逻辑
     *     } finally {
     *         cacheUtils.releaseLock("order:" + orderId, requestId);
     *     }
     * }
     * 
     * 缓存键命名规范：
     * 1. 用户相关：user:{userId}、user:session:{sessionId}
     * 2. 验证码：code:{type}:{target}
     * 3. 消息相关：message:{messageId}、chat:{chatId}
     * 4. 群组相关：group:{groupId}、group:members:{groupId}
     * 5. 文件相关：file:{fileId}、upload:temp:{uploadId}
     * 6. 统计计数：count:{type}:{date}、limit:{type}:{userId}
     * 
     * 注意事项：
     * 1. 缓存键会自动添加前缀，避免键冲突
     * 2. 大对象缓存会消耗较多内存，注意设置合理的过期时间
     * 3. 分布式锁使用完毕后必须释放，建议使用try-finally
     * 4. 缓存穿透、击穿、雪崩问题需要在业务层面处理
     * 5. Redis连接异常时操作会失败，需要有降级方案
     */

}