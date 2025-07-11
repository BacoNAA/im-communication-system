package com.im.imcommunicationsystem.relationship.constants;

/**
 * 联系人关系模块常量定义
 * 
 * @author System
 * @since 1.0.0
 */
public class RelationshipConstants {

    /**
     * 联系人关系常量
     */
    public static class Contact {
        /** 最大好友数量 */
        public static final int MAX_FRIENDS_COUNT = 5000;
        
        /** 最大备注长度 */
        public static final int MAX_REMARK_LENGTH = 50;
        
        /** 默认备注 */
        public static final String DEFAULT_REMARK = "";
        
        /** 联系人关系状态 - 正常 */
        public static final String STATUS_NORMAL = "NORMAL";
        
        /** 联系人关系状态 - 已屏蔽 */
        public static final String STATUS_BLOCKED = "BLOCKED";
        
        /** 联系人关系状态 - 已删除 */
        public static final String STATUS_DELETED = "DELETED";
    }

    /**
     * 好友请求常量
     */
    public static class Request {
        /** 请求消息最大长度 */
        public static final int MAX_MESSAGE_LENGTH = 200;
        
        /** 请求过期时间（天） */
        public static final int EXPIRY_DAYS = 30;
        
        /** 每日最大发送请求数量 */
        public static final int MAX_DAILY_REQUESTS = 50;
        
        /** 默认请求消息 */
        public static final String DEFAULT_MESSAGE = "我想加您为好友";
        
        /** 批量处理最大数量 */
        public static final int MAX_BATCH_SIZE = 100;
    }

    /**
     * 好友标签常量
     */
    public static class Tag {
        /** 标签名称最大长度 */
        public static final int MAX_NAME_LENGTH = 20;
        
        /** 标签名称最小长度 */
        public static final int MIN_NAME_LENGTH = 1;
        
        /** 每个用户最大标签数量 */
        public static final int MAX_TAGS_PER_USER = 50;
        
        /** 标签颜色长度 */
        public static final int COLOR_LENGTH = 7;
        
        /** 默认标签颜色列表 */
        public static final String[] DEFAULT_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
        };
        
        /** 默认标签名称 */
        public static final String DEFAULT_TAG_NAME = "默认分组";
    }

    /**
     * 缓存相关常量
     */
    public static class Cache {
        /** 联系人列表缓存键前缀 */
        public static final String CONTACT_LIST_PREFIX = "relationship:contact:list:";
        
        /** 好友请求列表缓存键前缀 */
        public static final String REQUEST_LIST_PREFIX = "relationship:request:list:";
        
        /** 标签列表缓存键前缀 */
        public static final String TAG_LIST_PREFIX = "relationship:tag:list:";
        
        /** 缓存过期时间（秒） */
        public static final long CACHE_EXPIRE_SECONDS = 3600;
        
        /** 短期缓存过期时间（秒） */
        public static final long SHORT_CACHE_EXPIRE_SECONDS = 300;
    }

    /**
     * 分页相关常量
     */
    public static class Page {
        /** 默认页面大小 */
        public static final int DEFAULT_SIZE = 20;
        
        /** 最大页面大小 */
        public static final int MAX_SIZE = 100;
        
        /** 默认页码 */
        public static final int DEFAULT_PAGE = 0;
    }

    /**
     * 权限相关常量
     */
    public static class Permission {
        /** 查看联系人权限 */
        public static final String VIEW_CONTACT = "relationship:contact:view";
        
        /** 管理联系人权限 */
        public static final String MANAGE_CONTACT = "relationship:contact:manage";
        
        /** 发送好友请求权限 */
        public static final String SEND_REQUEST = "relationship:request:send";
        
        /** 处理好友请求权限 */
        public static final String HANDLE_REQUEST = "relationship:request:handle";
        
        /** 管理标签权限 */
        public static final String MANAGE_TAG = "relationship:tag:manage";
    }

    /**
     * 错误码常量
     */
    public static class ErrorCode {
        /** 联系人相关错误码前缀 */
        public static final String CONTACT_PREFIX = "CONTACT_";
        
        /** 好友请求相关错误码前缀 */
        public static final String REQUEST_PREFIX = "REQUEST_";
        
        /** 标签相关错误码前缀 */
        public static final String TAG_PREFIX = "TAG_";
        
        /** 联系人不存在 */
        public static final String CONTACT_NOT_FOUND = CONTACT_PREFIX + "NOT_FOUND";
        
        /** 联系人已存在 */
        public static final String CONTACT_ALREADY_EXISTS = CONTACT_PREFIX + "ALREADY_EXISTS";
        
        /** 不能添加自己为好友 */
        public static final String CANNOT_ADD_SELF = CONTACT_PREFIX + "CANNOT_ADD_SELF";
        
        /** 联系人已被屏蔽 */
        public static final String CONTACT_BLOCKED = CONTACT_PREFIX + "BLOCKED";
        
        /** 好友请求不存在 */
        public static final String REQUEST_NOT_FOUND = REQUEST_PREFIX + "NOT_FOUND";
        
        /** 好友请求已存在 */
        public static final String REQUEST_ALREADY_EXISTS = REQUEST_PREFIX + "ALREADY_EXISTS";
        
        /** 标签不存在 */
        public static final String TAG_NOT_FOUND = TAG_PREFIX + "NOT_FOUND";
        
        /** 标签名称已存在 */
        public static final String TAG_NAME_EXISTS = TAG_PREFIX + "NAME_EXISTS";
    }

    /**
     * 正则表达式常量
     */
    public static class Regex {
        /** 标签名称正则表达式 */
        public static final String TAG_NAME_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]{1,20}$";
        
        /** 颜色代码正则表达式 */
        public static final String COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";
        
        /** 备注正则表达式 */
        public static final String REMARK_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]{0,50}$";
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RelationshipConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}