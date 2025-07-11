package com.im.imcommunicationsystem.relationship.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 联系人关系模块配置类
 * 
 * @author System
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.relationship")
@Validated
public class RelationshipConfig {

    /**
     * 联系人配置
     */
    @NotNull
    private Contact contact = new Contact();

    /**
     * 好友请求配置
     */
    @NotNull
    private Request request = new Request();

    /**
     * 好友标签配置
     */
    @NotNull
    private Tag tag = new Tag();

    /**
     * 缓存配置
     */
    @NotNull
    private Cache cache = new Cache();

    /**
     * 联系人配置类
     */
    @Data
    public static class Contact {
        /**
         * 最大好友数量
         */
        @Min(value = 100, message = "最大好友数量不能少于100")
        @Max(value = 10000, message = "最大好友数量不能超过10000")
        private Integer maxFriendsCount = 5000;

        /**
         * 最大备注长度
         */
        @Min(value = 10, message = "最大备注长度不能少于10")
        @Max(value = 100, message = "最大备注长度不能超过100")
        private Integer maxRemarkLength = 50;

        /**
         * 是否启用联系人搜索缓存
         */
        private Boolean enableSearchCache = true;

        /**
         * 联系人列表分页大小
         */
        @Min(value = 10, message = "分页大小不能少于10")
        @Max(value = 100, message = "分页大小不能超过100")
        private Integer pageSize = 20;
    }

    /**
     * 好友请求配置类
     */
    @Data
    public static class Request {
        /**
         * 请求消息最大长度
         */
        @Min(value = 50, message = "请求消息最大长度不能少于50")
        @Max(value = 500, message = "请求消息最大长度不能超过500")
        private Integer maxMessageLength = 200;

        /**
         * 请求过期时间（天）
         */
        @Min(value = 7, message = "请求过期时间不能少于7天")
        @Max(value = 90, message = "请求过期时间不能超过90天")
        private Integer expiryDays = 30;

        /**
         * 每日最大发送请求数量
         */
        @Min(value = 10, message = "每日最大发送请求数量不能少于10")
        @Max(value = 200, message = "每日最大发送请求数量不能超过200")
        private Integer maxDailyRequests = 50;

        /**
         * 批量处理最大数量
         */
        @Min(value = 10, message = "批量处理最大数量不能少于10")
        @Max(value = 500, message = "批量处理最大数量不能超过500")
        private Integer maxBatchSize = 100;

        /**
         * 是否启用请求频率限制
         */
        private Boolean enableRateLimit = true;

        /**
         * 是否自动清理过期请求
         */
        private Boolean autoCleanExpired = true;

        /**
         * 自动清理间隔（小时）
         */
        @Min(value = 1, message = "自动清理间隔不能少于1小时")
        @Max(value = 168, message = "自动清理间隔不能超过168小时")
        private Integer cleanIntervalHours = 24;
    }

    /**
     * 好友标签配置类
     */
    @Data
    public static class Tag {
        /**
         * 标签名称最大长度
         */
        @Min(value = 10, message = "标签名称最大长度不能少于10")
        @Max(value = 50, message = "标签名称最大长度不能超过50")
        private Integer maxNameLength = 20;

        /**
         * 每个用户最大标签数量
         */
        @Min(value = 10, message = "每个用户最大标签数量不能少于10")
        @Max(value = 200, message = "每个用户最大标签数量不能超过200")
        private Integer maxTagsPerUser = 50;

        /**
         * 是否启用默认标签
         */
        private Boolean enableDefaultTag = true;

        /**
         * 默认标签名称
         */
        private String defaultTagName = "默认分组";

        /**
         * 默认标签颜色
         */
        private String defaultTagColor = "#4ECDC4";

        /**
         * 是否允许删除默认标签
         */
        private Boolean allowDeleteDefault = false;
    }

    /**
     * 缓存配置类
     */
    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private Boolean enabled = true;

        /**
         * 缓存过期时间（秒）
         */
        @Min(value = 300, message = "缓存过期时间不能少于300秒")
        @Max(value = 86400, message = "缓存过期时间不能超过86400秒")
        private Long expireSeconds = 3600L;

        /**
         * 短期缓存过期时间（秒）
         */
        @Min(value = 60, message = "短期缓存过期时间不能少于60秒")
        @Max(value = 3600, message = "短期缓存过期时间不能超过3600秒")
        private Long shortExpireSeconds = 300L;

        /**
         * 缓存键前缀
         */
        private String keyPrefix = "relationship";

        /**
         * 是否启用本地缓存
         */
        private Boolean enableLocalCache = false;

        /**
         * 本地缓存最大条目数
         */
        @Min(value = 100, message = "本地缓存最大条目数不能少于100")
        @Max(value = 10000, message = "本地缓存最大条目数不能超过10000")
        private Integer localCacheMaxSize = 1000;
    }
}