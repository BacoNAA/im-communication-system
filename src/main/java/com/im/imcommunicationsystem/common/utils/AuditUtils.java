package com.im.imcommunicationsystem.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 审计工具类
 * 提供用户操作日志记录和系统审计功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class AuditUtils {

    @Autowired
    private CacheUtils cacheUtils;

    /**
     * 异步执行器
     */
    private static final Executor AUDIT_EXECUTOR = Executors.newFixedThreadPool(2);

    /**
     * 审计日志缓存前缀
     */
    private static final String AUDIT_CACHE_PREFIX = "audit:";

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        // 用户操作
        USER_LOGIN("用户登录"),
        USER_LOGOUT("用户登出"),
        USER_REGISTER("用户注册"),
        USER_UPDATE_PROFILE("更新用户资料"),
        USER_CHANGE_PASSWORD("修改密码"),
        USER_RESET_PASSWORD("重置密码"),
        
        // 消息操作
        MESSAGE_SEND("发送消息"),
        MESSAGE_RECALL("撤回消息"),
        MESSAGE_DELETE("删除消息"),
        MESSAGE_FORWARD("转发消息"),
        
        // 群组操作
        GROUP_CREATE("创建群组"),
        GROUP_JOIN("加入群组"),
        GROUP_LEAVE("退出群组"),
        GROUP_UPDATE("更新群组信息"),
        GROUP_DELETE("删除群组"),
        GROUP_INVITE("邀请入群"),
        GROUP_REMOVE_MEMBER("移除群成员"),
        GROUP_SET_ADMIN("设置管理员"),
        GROUP_UNSET_ADMIN("取消管理员"),
        
        // 好友操作
        FRIEND_ADD("添加好友"),
        FRIEND_DELETE("删除好友"),
        FRIEND_BLOCK("拉黑好友"),
        FRIEND_UNBLOCK("取消拉黑"),
        
        // 文件操作
        FILE_UPLOAD("文件上传"),
        FILE_DOWNLOAD("文件下载"),
        FILE_DELETE("文件删除"),
        
        // 系统操作
        SYSTEM_CONFIG_UPDATE("系统配置更新"),
        SYSTEM_MAINTENANCE("系统维护"),
        SYSTEM_BACKUP("系统备份"),
        
        // 安全操作
        SECURITY_LOGIN_FAIL("登录失败"),
        SECURITY_ACCOUNT_LOCKED("账户锁定"),
        SECURITY_PERMISSION_DENIED("权限拒绝"),
        SECURITY_SUSPICIOUS_ACTIVITY("可疑活动");
        
        private final String description;
        
        OperationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS("成功"),
        FAILURE("失败"),
        PARTIAL_SUCCESS("部分成功"),
        CANCELLED("已取消"),
        TIMEOUT("超时");
        
        private final String description;
        
        OperationResult(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * 审计日志实体
     */
    @Data
    public static class AuditLog {
        /**
         * 日志ID
         */
        private String logId;
        
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户名
         */
        private String username;
        
        /**
         * 操作类型
         */
        private OperationType operationType;
        
        /**
         * 操作描述
         */
        private String operationDescription;
        
        /**
         * 操作结果
         */
        private OperationResult operationResult;
        
        /**
         * 目标资源ID
         */
        private String resourceId;
        
        /**
         * 目标资源类型
         */
        private String resourceType;
        
        /**
         * 操作详情
         */
        private Map<String, Object> details;
        
        /**
         * 客户端IP
         */
        private String clientIp;
        
        /**
         * 用户代理
         */
        private String userAgent;
        
        /**
         * 请求URL
         */
        private String requestUrl;
        
        /**
         * 请求方法
         */
        private String requestMethod;
        
        /**
         * 操作时间
         */
        private LocalDateTime operationTime;
        
        /**
         * 执行耗时（毫秒）
         */
        private Long executionTime;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        /**
         * 会话ID
         */
        private String sessionId;
        
        /**
         * 设备信息
         */
        private String deviceInfo;
        
        /**
         * 地理位置
         */
        private String location;
    }

    /**
     * 记录操作日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param operationResult 操作结果
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     */
    public void logOperation(Long userId, String username, OperationType operationType, 
                           OperationResult operationResult, String resourceId, String resourceType) {
        logOperation(userId, username, operationType, operationResult, resourceId, resourceType, null, null);
    }

    /**
     * 记录操作日志（带详情）
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param operationResult 操作结果
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     * @param details 操作详情
     * @param errorMessage 错误信息
     */
    public void logOperation(Long userId, String username, OperationType operationType, 
                           OperationResult operationResult, String resourceId, String resourceType,
                           Map<String, Object> details, String errorMessage) {
        try {
            AuditLog auditLog = createAuditLog(userId, username, operationType, operationResult, 
                                             resourceId, resourceType, details, errorMessage);
            
            // 异步记录日志
            CompletableFuture.runAsync(() -> {
                saveAuditLog(auditLog);
            }, AUDIT_EXECUTOR);
            
        } catch (Exception e) {
            log.error("记录审计日志失败: userId={}, operationType={}", userId, operationType, e);
        }
    }

    /**
     * 记录登录日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    public void logLogin(Long userId, String username, boolean success, String errorMessage) {
        OperationType operationType = success ? OperationType.USER_LOGIN : OperationType.SECURITY_LOGIN_FAIL;
        OperationResult operationResult = success ? OperationResult.SUCCESS : OperationResult.FAILURE;
        
        Map<String, Object> details = new HashMap<>();
        details.put("loginTime", DateTimeUtils.getCurrentDateTime());
        details.put("success", success);
        
        logOperation(userId, username, operationType, operationResult, 
                    String.valueOf(userId), "USER", details, errorMessage);
    }

    /**
     * 记录登出日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     */
    public void logLogout(Long userId, String username) {
        Map<String, Object> details = new HashMap<>();
        details.put("logoutTime", DateTimeUtils.getCurrentDateTime());
        
        logOperation(userId, username, OperationType.USER_LOGOUT, OperationResult.SUCCESS, 
                    String.valueOf(userId), "USER", details, null);
    }

    /**
     * 记录消息发送日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param messageId 消息ID
     * @param messageType 消息类型
     * @param targetId 目标ID（用户ID或群组ID）
     * @param success 是否成功
     */
    public void logMessageSend(Long userId, String username, String messageId, 
                              String messageType, String targetId, boolean success) {
        Map<String, Object> details = new HashMap<>();
        details.put("messageType", messageType);
        details.put("targetId", targetId);
        details.put("sendTime", DateTimeUtils.getCurrentDateTime());
        
        OperationResult result = success ? OperationResult.SUCCESS : OperationResult.FAILURE;
        
        logOperation(userId, username, OperationType.MESSAGE_SEND, result, 
                    messageId, "MESSAGE", details, null);
    }

    /**
     * 记录群组操作日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param groupId 群组ID
     * @param targetUserId 目标用户ID（可选）
     * @param success 是否成功
     */
    public void logGroupOperation(Long userId, String username, OperationType operationType, 
                                 String groupId, Long targetUserId, boolean success) {
        Map<String, Object> details = new HashMap<>();
        details.put("groupId", groupId);
        if (targetUserId != null) {
            details.put("targetUserId", targetUserId);
        }
        details.put("operationTime", DateTimeUtils.getCurrentDateTime());
        
        OperationResult result = success ? OperationResult.SUCCESS : OperationResult.FAILURE;
        
        logOperation(userId, username, operationType, result, 
                    groupId, "GROUP", details, null);
    }

    /**
     * 记录文件操作日志
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param fileId 文件ID
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param success 是否成功
     */
    public void logFileOperation(Long userId, String username, OperationType operationType, 
                                String fileId, String fileName, Long fileSize, boolean success) {
        Map<String, Object> details = new HashMap<>();
        details.put("fileName", fileName);
        details.put("fileSize", fileSize);
        details.put("operationTime", DateTimeUtils.getCurrentDateTime());
        
        OperationResult result = success ? OperationResult.SUCCESS : OperationResult.FAILURE;
        
        logOperation(userId, username, operationType, result, 
                    fileId, "FILE", details, null);
    }

    /**
     * 记录安全事件
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param description 事件描述
     * @param riskLevel 风险级别
     */
    public void logSecurityEvent(Long userId, String username, OperationType operationType, 
                                String description, String riskLevel) {
        Map<String, Object> details = new HashMap<>();
        details.put("description", description);
        details.put("riskLevel", riskLevel);
        details.put("eventTime", DateTimeUtils.getCurrentDateTime());
        
        logOperation(userId, username, operationType, OperationResult.FAILURE, 
                    String.valueOf(userId), "SECURITY", details, description);
        
        // 高风险事件立即告警
        if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
            alertSecurityEvent(userId, username, operationType, description);
        }
    }

    /**
     * 创建审计日志对象
     */
    private AuditLog createAuditLog(Long userId, String username, OperationType operationType, 
                                   OperationResult operationResult, String resourceId, String resourceType,
                                   Map<String, Object> details, String errorMessage) {
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(EncryptionUtils.generateUUID());
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setOperationType(operationType);
        auditLog.setOperationDescription(operationType.getDescription());
        auditLog.setOperationResult(operationResult);
        auditLog.setResourceId(resourceId);
        auditLog.setResourceType(resourceType);
        auditLog.setDetails(details != null ? details : new HashMap<>());
        auditLog.setOperationTime(DateTimeUtils.getCurrentDateTime());
        auditLog.setErrorMessage(errorMessage);
        
        // 获取请求信息
        HttpServletRequest request = HttpUtils.getRequest();
        if (request != null) {
            auditLog.setClientIp(NetworkUtils.getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setRequestUrl(HttpUtils.getFullUrl(request));
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setSessionId(request.getSession().getId());
            
            // 设备信息
            String userAgent = request.getHeader("User-Agent");
            if (StringUtils.hasText(userAgent)) {
                auditLog.setDeviceInfo(parseDeviceInfo(userAgent));
            }
        }
        
        return auditLog;
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(AuditLog auditLog) {
        try {
            // 保存到缓存（用于实时查询）
            String cacheKey = AUDIT_CACHE_PREFIX + "log:" + auditLog.getLogId();
            cacheUtils.set(cacheKey, auditLog, CacheUtils.LONG_EXPIRE);
            
            // 保存到用户操作历史
            String userHistoryKey = AUDIT_CACHE_PREFIX + "user:" + auditLog.getUserId();
            cacheUtils.lPush(userHistoryKey, auditLog.getLogId());
            cacheUtils.expire(userHistoryKey, CacheUtils.LONG_EXPIRE * 7); // 保存7天
            
            // 保存到操作类型统计
            String typeCountKey = AUDIT_CACHE_PREFIX + "count:" + auditLog.getOperationType().name() + ":" + 
                                 DateTimeUtils.formatDate(auditLog.getOperationTime(), "yyyy-MM-dd");
            cacheUtils.increment(typeCountKey);
            cacheUtils.expire(typeCountKey, CacheUtils.LONG_EXPIRE * 30); // 保存30天
            
            // 记录结构化日志
            log.info("审计日志: userId={}, username={}, operation={}, result={}, resource={}:{}, ip={}, time={}",
                    auditLog.getUserId(), auditLog.getUsername(), auditLog.getOperationType(),
                    auditLog.getOperationResult(), auditLog.getResourceType(), auditLog.getResourceId(),
                    auditLog.getClientIp(), auditLog.getOperationTime());
            
        } catch (Exception e) {
            log.error("保存审计日志失败: logId={}", auditLog.getLogId(), e);
        }
    }

    /**
     * 解析设备信息
     */
    private String parseDeviceInfo(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        
        userAgent = userAgent.toLowerCase();
        
        // 移动设备
        if (userAgent.contains("mobile") || userAgent.contains("android") || 
            userAgent.contains("iphone") || userAgent.contains("ipad")) {
            if (userAgent.contains("android")) {
                return "Android Mobile";
            } else if (userAgent.contains("iphone")) {
                return "iPhone";
            } else if (userAgent.contains("ipad")) {
                return "iPad";
            } else {
                return "Mobile Device";
            }
        }
        
        // 桌面设备
        if (userAgent.contains("windows")) {
            return "Windows Desktop";
        } else if (userAgent.contains("mac")) {
            return "Mac Desktop";
        } else if (userAgent.contains("linux")) {
            return "Linux Desktop";
        }
        
        return "Unknown Device";
    }

    /**
     * 安全事件告警
     */
    private void alertSecurityEvent(Long userId, String username, OperationType operationType, String description) {
        try {
            // 记录高风险事件
            String alertKey = AUDIT_CACHE_PREFIX + "alert:" + DateTimeUtils.getCurrentDateTimeStr();
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("userId", userId);
            alertData.put("username", username);
            alertData.put("operationType", operationType.name());
            alertData.put("description", description);
            alertData.put("alertTime", DateTimeUtils.getCurrentDateTime());
            
            cacheUtils.set(alertKey, alertData, CacheUtils.LONG_EXPIRE);
            
            // 发送告警通知（这里可以集成邮件、短信、钉钉等通知方式）
            log.warn("安全告警: userId={}, username={}, operation={}, description={}", 
                    userId, username, operationType, description);
            
        } catch (Exception e) {
            log.error("发送安全告警失败: userId={}, operationType={}", userId, operationType, e);
        }
    }

    /**
     * 获取用户操作历史
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 操作历史列表
     */
    public java.util.List<AuditLog> getUserOperationHistory(Long userId, int limit) {
        try {
            String userHistoryKey = AUDIT_CACHE_PREFIX + "user:" + userId;
            java.util.List<Object> logIds = cacheUtils.lRange(userHistoryKey, 0, limit - 1);
            
            java.util.List<AuditLog> auditLogs = new java.util.ArrayList<>();
            for (Object logId : logIds) {
                String cacheKey = AUDIT_CACHE_PREFIX + "log:" + logId;
                AuditLog auditLog = cacheUtils.get(cacheKey, AuditLog.class);
                if (auditLog != null) {
                    auditLogs.add(auditLog);
                }
            }
            
            return auditLogs;
        } catch (Exception e) {
            log.error("获取用户操作历史失败: userId={}", userId, e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 获取操作统计
     * 
     * @param operationType 操作类型
     * @param date 日期
     * @return 操作次数
     */
    public long getOperationCount(OperationType operationType, LocalDateTime date) {
        try {
            String typeCountKey = AUDIT_CACHE_PREFIX + "count:" + operationType.name() + ":" + 
                                 DateTimeUtils.formatDate(date, "yyyy-MM-dd");
            Object count = cacheUtils.get(typeCountKey);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            log.error("获取操作统计失败: operationType={}, date={}", operationType, date, e);
            return 0;
        }
    }

    /**
     * 审计工具使用说明：
     * 
     * 1. 基础日志记录：
     *    - logOperation() - 记录通用操作日志
     *    - logLogin() - 记录登录日志
     *    - logLogout() - 记录登出日志
     * 
     * 2. 业务日志记录：
     *    - logMessageSend() - 记录消息发送日志
     *    - logGroupOperation() - 记录群组操作日志
     *    - logFileOperation() - 记录文件操作日志
     * 
     * 3. 安全日志记录：
     *    - logSecurityEvent() - 记录安全事件
     * 
     * 4. 查询功能：
     *    - getUserOperationHistory() - 获取用户操作历史
     *    - getOperationCount() - 获取操作统计
     * 
     * 使用示例：
     * 
     * // 记录用户登录
     * auditUtils.logLogin(userId, username, true, null);
     * 
     * // 记录消息发送
     * auditUtils.logMessageSend(userId, username, messageId, "TEXT", targetId, true);
     * 
     * // 记录群组操作
     * auditUtils.logGroupOperation(userId, username, OperationType.GROUP_JOIN, groupId, null, true);
     * 
     * // 记录安全事件
     * auditUtils.logSecurityEvent(userId, username, OperationType.SECURITY_LOGIN_FAIL, 
     *                            "连续登录失败", "HIGH");
     * 
     * // 获取用户操作历史
     * List<AuditLog> history = auditUtils.getUserOperationHistory(userId, 50);
     * 
     * 配置说明：
     * 1. 日志异步记录，不影响业务性能
     * 2. 缓存保存7-30天，可根据需要调整
     * 3. 高风险安全事件会立即告警
     * 4. 支持结构化日志输出
     * 
     * 注意事项：
     * 1. 敏感信息（如密码）不应记录在日志中
     * 2. 大量日志可能影响存储性能，需要定期清理
     * 3. 审计日志应具有不可篡改性
     * 4. 关键操作必须记录审计日志
     * 5. 日志记录失败不应影响业务流程
     */

}