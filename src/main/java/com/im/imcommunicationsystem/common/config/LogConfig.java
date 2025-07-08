package com.im.imcommunicationsystem.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 日志配置类
 * 提供统一的日志管理配置
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@Component
@ConfigurationProperties(prefix = "app.log")
public class LogConfig {

    /**
     * 是否启用访问日志
     */
    private boolean enableAccessLog = true;

    /**
     * 是否启用操作日志
     */
    private boolean enableOperationLog = true;

    /**
     * 是否启用错误日志
     */
    private boolean enableErrorLog = true;

    /**
     * 是否启用性能日志
     */
    private boolean enablePerformanceLog = false;

    /**
     * 是否启用SQL日志
     */
    private boolean enableSqlLog = false;

    /**
     * 慢查询阈值（毫秒）
     */
    private long slowQueryThreshold = 1000;

    /**
     * 慢接口阈值（毫秒）
     */
    private long slowApiThreshold = 3000;

    /**
     * 日志保留天数
     */
    private int retentionDays = 30;

    /**
     * 最大日志文件大小
     */
    private String maxFileSize = "100MB";

    /**
     * 日志文件路径
     */
    private String logPath = "./logs";

    /**
     * 是否启用异步日志
     */
    private boolean enableAsyncLog = true;

    /**
     * 异步日志队列大小
     */
    private int asyncQueueSize = 1024;

    /**
     * 是否记录请求参数
     */
    private boolean logRequestParams = true;

    /**
     * 是否记录响应结果
     */
    private boolean logResponseResult = false;

    /**
     * 敏感字段列表（不记录到日志中）
     */
    private String[] sensitiveFields = {
            "password", "pwd", "secret", "token", "key", "authorization",
            "credit_card", "ssn", "phone", "email", "id_card"
    };

    /**
     * 日志级别配置
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * 根日志级别
     */
    private LogLevel rootLevel = LogLevel.INFO;

    /**
     * 应用日志级别
     */
    private LogLevel appLevel = LogLevel.INFO;

    /**
     * SQL日志级别
     */
    private LogLevel sqlLevel = LogLevel.DEBUG;

    /**
     * 第三方库日志级别
     */
    private LogLevel thirdPartyLevel = LogLevel.WARN;

    // Getter and Setter methods

    public boolean isEnableAccessLog() {
        return enableAccessLog;
    }

    public void setEnableAccessLog(boolean enableAccessLog) {
        this.enableAccessLog = enableAccessLog;
    }

    public boolean isEnableOperationLog() {
        return enableOperationLog;
    }

    public void setEnableOperationLog(boolean enableOperationLog) {
        this.enableOperationLog = enableOperationLog;
    }

    public boolean isEnableErrorLog() {
        return enableErrorLog;
    }

    public void setEnableErrorLog(boolean enableErrorLog) {
        this.enableErrorLog = enableErrorLog;
    }

    public boolean isEnablePerformanceLog() {
        return enablePerformanceLog;
    }

    public void setEnablePerformanceLog(boolean enablePerformanceLog) {
        this.enablePerformanceLog = enablePerformanceLog;
    }

    public boolean isEnableSqlLog() {
        return enableSqlLog;
    }

    public void setEnableSqlLog(boolean enableSqlLog) {
        this.enableSqlLog = enableSqlLog;
    }

    public long getSlowQueryThreshold() {
        return slowQueryThreshold;
    }

    public void setSlowQueryThreshold(long slowQueryThreshold) {
        this.slowQueryThreshold = slowQueryThreshold;
    }

    public long getSlowApiThreshold() {
        return slowApiThreshold;
    }

    public void setSlowApiThreshold(long slowApiThreshold) {
        this.slowApiThreshold = slowApiThreshold;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public boolean isEnableAsyncLog() {
        return enableAsyncLog;
    }

    public void setEnableAsyncLog(boolean enableAsyncLog) {
        this.enableAsyncLog = enableAsyncLog;
    }

    public int getAsyncQueueSize() {
        return asyncQueueSize;
    }

    public void setAsyncQueueSize(int asyncQueueSize) {
        this.asyncQueueSize = asyncQueueSize;
    }

    public boolean isLogRequestParams() {
        return logRequestParams;
    }

    public void setLogRequestParams(boolean logRequestParams) {
        this.logRequestParams = logRequestParams;
    }

    public boolean isLogResponseResult() {
        return logResponseResult;
    }

    public void setLogResponseResult(boolean logResponseResult) {
        this.logResponseResult = logResponseResult;
    }

    public String[] getSensitiveFields() {
        return sensitiveFields;
    }

    public void setSensitiveFields(String[] sensitiveFields) {
        this.sensitiveFields = sensitiveFields;
    }

    public LogLevel getRootLevel() {
        return rootLevel;
    }

    public void setRootLevel(LogLevel rootLevel) {
        this.rootLevel = rootLevel;
    }

    public LogLevel getAppLevel() {
        return appLevel;
    }

    public void setAppLevel(LogLevel appLevel) {
        this.appLevel = appLevel;
    }

    public LogLevel getSqlLevel() {
        return sqlLevel;
    }

    public void setSqlLevel(LogLevel sqlLevel) {
        this.sqlLevel = sqlLevel;
    }

    public LogLevel getThirdPartyLevel() {
        return thirdPartyLevel;
    }

    public void setThirdPartyLevel(LogLevel thirdPartyLevel) {
        this.thirdPartyLevel = thirdPartyLevel;
    }

    /**
     * 检查是否为敏感字段
     * 
     * @param fieldName 字段名
     * @return 是否为敏感字段
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null || sensitiveFields == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        for (String sensitiveField : sensitiveFields) {
            if (lowerFieldName.contains(sensitiveField.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取日志配置信息
     * 
     * @return 配置信息字符串
     */
    public String getConfigInfo() {
        return String.format(
                "LogConfig{enableAccessLog=%s, enableOperationLog=%s, enableErrorLog=%s, " +
                "enablePerformanceLog=%s, enableSqlLog=%s, slowQueryThreshold=%d, " +
                "slowApiThreshold=%d, retentionDays=%d, maxFileSize='%s', logPath='%s', " +
                "enableAsyncLog=%s, asyncQueueSize=%d, logRequestParams=%s, " +
                "logResponseResult=%s, rootLevel=%s, appLevel=%s}",
                enableAccessLog, enableOperationLog, enableErrorLog,
                enablePerformanceLog, enableSqlLog, slowQueryThreshold,
                slowApiThreshold, retentionDays, maxFileSize, logPath,
                enableAsyncLog, asyncQueueSize, logRequestParams,
                logResponseResult, rootLevel, appLevel
        );
    }

    /**
     * 日志配置使用说明：
     * 
     * 1. 配置文件设置（application.yml）：
     * app:
     *   log:
     *     enable-access-log: true
     *     enable-operation-log: true
     *     enable-error-log: true
     *     enable-performance-log: false
     *     enable-sql-log: false
     *     slow-query-threshold: 1000
     *     slow-api-threshold: 3000
     *     retention-days: 30
     *     max-file-size: 100MB
     *     log-path: ./logs
     *     enable-async-log: true
     *     async-queue-size: 1024
     *     log-request-params: true
     *     log-response-result: false
     *     sensitive-fields:
     *       - password
     *       - token
     *       - secret
     *     root-level: INFO
     *     app-level: INFO
     *     sql-level: DEBUG
     *     third-party-level: WARN
     * 
     * 2. 日志类型说明：
     *    - 访问日志：记录HTTP请求和响应
     *    - 操作日志：记录用户操作行为
     *    - 错误日志：记录系统异常和错误
     *    - 性能日志：记录接口响应时间和性能指标
     *    - SQL日志：记录数据库查询语句
     * 
     * 3. 性能配置：
     *    - 慢查询阈值：超过此时间的SQL查询会被记录
     *    - 慢接口阈值：超过此时间的API请求会被记录
     *    - 异步日志：提高日志写入性能
     * 
     * 4. 安全配置：
     *    - 敏感字段：配置不应记录到日志中的字段名
     *    - 请求参数记录：是否记录请求参数
     *    - 响应结果记录：是否记录响应结果
     * 
     * 5. 文件管理：
     *    - 日志保留天数：自动清理过期日志
     *    - 最大文件大小：单个日志文件的最大大小
     *    - 日志路径：日志文件存储路径
     * 
     * 使用示例：
     * 
     * @Autowired
     * private LogConfig logConfig;
     * 
     * // 检查是否启用访问日志
     * if (logConfig.isEnableAccessLog()) {
     *     log.info("记录访问日志: {}", request.getRequestURI());
     * }
     * 
     * // 检查是否为敏感字段
     * if (!logConfig.isSensitiveField("password")) {
     *     log.info("参数值: {}", value);
     * }
     * 
     * // 性能日志
     * long duration = System.currentTimeMillis() - startTime;
     * if (duration > logConfig.getSlowApiThreshold()) {
     *     log.warn("慢接口: {} 耗时: {}ms", api, duration);
     * }
     * 
     * 注意事项：
     * 1. 生产环境建议关闭SQL日志和详细的请求响应日志
     * 2. 敏感信息不应记录到日志中
     * 3. 异步日志可以提高性能，但可能在系统崩溃时丢失部分日志
     * 4. 日志文件大小和保留时间需要根据磁盘空间合理配置
     * 5. 不同环境可以使用不同的日志级别配置
     */

}