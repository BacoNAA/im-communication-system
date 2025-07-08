package com.im.imcommunicationsystem.common.constants;

/**
 * 通用常量类
 * 定义系统中使用的各种常量
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
public class CommonConstants {

    /**
     * 系统信息常量
     */
    public static class System {
        /** 系统名称 */
        public static final String NAME = "IM Communication System";
        
        /** 系统版本 */
        public static final String VERSION = "1.0.0";
        
        /** 系统编码 */
        public static final String CHARSET = "UTF-8";
        
        /** 默认时区 */
        public static final String TIMEZONE = "Asia/Shanghai";
        
        /** 默认语言 */
        public static final String DEFAULT_LANGUAGE = "zh-CN";
    }

    /**
     * HTTP相关常量
     */
    public static class Http {
        /** 请求头 - 授权 */
        public static final String HEADER_AUTHORIZATION = "Authorization";
        
        /** 请求头 - 内容类型 */
        public static final String HEADER_CONTENT_TYPE = "Content-Type";
        
        /** 请求头 - 用户代理 */
        public static final String HEADER_USER_AGENT = "User-Agent";
        
        /** 请求头 - 客户端IP */
        public static final String HEADER_CLIENT_IP = "X-Real-IP";
        
        /** Bearer令牌前缀 */
        public static final String BEARER_PREFIX = "Bearer ";
        
        /** JSON内容类型 */
        public static final String CONTENT_TYPE_JSON = "application/json";
        
        /** 表单内容类型 */
        public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
        
        /** 文件上传内容类型 */
        public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    }

    /**
     * 响应码常量
     */
    public static class ResponseCode {
        /** 成功 */
        public static final int SUCCESS = 200;
        
        /** 创建成功 */
        public static final int CREATED = 201;
        
        /** 无内容 */
        public static final int NO_CONTENT = 204;
        
        /** 请求错误 */
        public static final int BAD_REQUEST = 400;
        
        /** 未授权 */
        public static final int UNAUTHORIZED = 401;
        
        /** 权限不足 */
        public static final int FORBIDDEN = 403;
        
        /** 资源不存在 */
        public static final int NOT_FOUND = 404;
        
        /** 方法不允许 */
        public static final int METHOD_NOT_ALLOWED = 405;
        
        /** 资源冲突 */
        public static final int CONFLICT = 409;
        
        /** 参数验证失败 */
        public static final int UNPROCESSABLE_ENTITY = 422;
        
        /** 服务器错误 */
        public static final int INTERNAL_SERVER_ERROR = 500;
        
        /** 网关错误 */
        public static final int BAD_GATEWAY = 502;
        
        /** 服务不可用 */
        public static final int SERVICE_UNAVAILABLE = 503;
    }

    /**
     * 用户相关常量
     */
    public static class User {
        /** 默认头像 */
        public static final String DEFAULT_AVATAR = "/images/default-avatar.png";
        
        /** 用户名最小长度 */
        public static final int USERNAME_MIN_LENGTH = 3;
        
        /** 用户名最大长度 */
        public static final int USERNAME_MAX_LENGTH = 20;
        
        /** 密码最小长度 */
        public static final int PASSWORD_MIN_LENGTH = 6;
        
        /** 密码最大长度 */
        public static final int PASSWORD_MAX_LENGTH = 20;
        
        /** 昵称最大长度 */
        public static final int NICKNAME_MAX_LENGTH = 50;
        
        /** 个性签名最大长度 */
        public static final int SIGNATURE_MAX_LENGTH = 200;
        
        /** 用户状态 - 正常 */
        public static final int STATUS_NORMAL = 1;
        
        /** 用户状态 - 禁用 */
        public static final int STATUS_DISABLED = 0;
        
        /** 用户状态 - 删除 */
        public static final int STATUS_DELETED = -1;
    }

    /**
     * 角色权限常量
     */
    public static class Role {
        /** 超级管理员 */
        public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
        
        /** 管理员 */
        public static final String ADMIN = "ROLE_ADMIN";
        
        /** 版主 */
        public static final String MODERATOR = "ROLE_MODERATOR";
        
        /** 普通用户 */
        public static final String USER = "ROLE_USER";
        
        /** 访客 */
        public static final String GUEST = "ROLE_GUEST";
    }

    /**
     * 消息相关常量
     */
    public static class Message {
        /** 消息类型 - 文本 */
        public static final String TYPE_TEXT = "TEXT";
        
        /** 消息类型 - 图片 */
        public static final String TYPE_IMAGE = "IMAGE";
        
        /** 消息类型 - 视频 */
        public static final String TYPE_VIDEO = "VIDEO";
        
        /** 消息类型 - 音频 */
        public static final String TYPE_AUDIO = "AUDIO";
        
        /** 消息类型 - 文件 */
        public static final String TYPE_FILE = "FILE";
        
        /** 消息类型 - 系统消息 */
        public static final String TYPE_SYSTEM = "SYSTEM";
        
        /** 消息类型 - 通知 */
        public static final String TYPE_NOTIFICATION = "NOTIFICATION";
        
        /** 消息状态 - 发送中 */
        public static final int STATUS_SENDING = 0;
        
        /** 消息状态 - 已发送 */
        public static final int STATUS_SENT = 1;
        
        /** 消息状态 - 已送达 */
        public static final int STATUS_DELIVERED = 2;
        
        /** 消息状态 - 已读 */
        public static final int STATUS_READ = 3;
        
        /** 消息状态 - 已撤回 */
        public static final int STATUS_RECALLED = 4;
        
        /** 消息状态 - 发送失败 */
        public static final int STATUS_FAILED = -1;
        
        /** 文本消息最大长度 */
        public static final int TEXT_MAX_LENGTH = 2000;
        
        /** 消息撤回时间限制（分钟） */
        public static final int RECALL_TIME_LIMIT = 2;
    }

    /**
     * 群组相关常量
     */
    public static class Group {
        /** 群组类型 - 普通群 */
        public static final String TYPE_NORMAL = "NORMAL";
        
        /** 群组类型 - 讨论组 */
        public static final String TYPE_DISCUSSION = "DISCUSSION";
        
        /** 群组类型 - 系统群 */
        public static final String TYPE_SYSTEM = "SYSTEM";
        
        /** 群组状态 - 正常 */
        public static final int STATUS_NORMAL = 1;
        
        /** 群组状态 - 禁用 */
        public static final int STATUS_DISABLED = 0;
        
        /** 群组状态 - 解散 */
        public static final int STATUS_DISSOLVED = -1;
        
        /** 成员角色 - 群主 */
        public static final String MEMBER_ROLE_OWNER = "OWNER";
        
        /** 成员角色 - 管理员 */
        public static final String MEMBER_ROLE_ADMIN = "ADMIN";
        
        /** 成员角色 - 普通成员 */
        public static final String MEMBER_ROLE_MEMBER = "MEMBER";
        
        /** 群组名称最大长度 */
        public static final int NAME_MAX_LENGTH = 50;
        
        /** 群组描述最大长度 */
        public static final int DESCRIPTION_MAX_LENGTH = 500;
        
        /** 群组最大成员数 */
        public static final int MAX_MEMBERS = 500;
        
        /** 默认群头像 */
        public static final String DEFAULT_AVATAR = "/images/default-group-avatar.png";
    }

    /**
     * 文件相关常量
     */
    public static class File {
        /** 文件上传最大大小（10MB） */
        public static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024;
        
        /** 图片文件最大大小（5MB） */
        public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
        
        /** 视频文件最大大小（100MB） */
        public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;
        
        /** 音频文件最大大小（20MB） */
        public static final long MAX_AUDIO_SIZE = 20 * 1024 * 1024;
        
        /** 文档文件最大大小（50MB） */
        public static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;
        
        /** 文件名最大长度 */
        public static final int FILENAME_MAX_LENGTH = 255;
        
        /** 文件存储根目录 */
        public static final String UPLOAD_ROOT = "/uploads";
        
        /** 图片存储目录 */
        public static final String IMAGE_DIR = "images";
        
        /** 视频存储目录 */
        public static final String VIDEO_DIR = "videos";
        
        /** 音频存储目录 */
        public static final String AUDIO_DIR = "audios";
        
        /** 文档存储目录 */
        public static final String DOCUMENT_DIR = "documents";
        
        /** 其他文件存储目录 */
        public static final String OTHER_DIR = "others";
    }

    /**
     * 缓存相关常量
     */
    public static class Cache {
        /** 用户信息缓存前缀 */
        public static final String USER_INFO_PREFIX = "user:info:";
        
        /** 用户会话缓存前缀 */
        public static final String USER_SESSION_PREFIX = "user:session:";
        
        /** 用户在线状态缓存前缀 */
        public static final String USER_ONLINE_PREFIX = "user:online:";
        
        /** 群组信息缓存前缀 */
        public static final String GROUP_INFO_PREFIX = "group:info:";
        
        /** 群组成员缓存前缀 */
        public static final String GROUP_MEMBERS_PREFIX = "group:members:";
        
        /** 验证码缓存前缀 */
        public static final String VERIFICATION_CODE_PREFIX = "verification:code:";
        
        /** 消息缓存前缀 */
        public static final String MESSAGE_PREFIX = "message:";
        
        /** 会话缓存前缀 */
        public static final String CONVERSATION_PREFIX = "conversation:";
        
        /** 默认缓存过期时间（秒） */
        public static final long DEFAULT_EXPIRE_TIME = 3600;
        
        /** 短期缓存过期时间（秒） */
        public static final long SHORT_EXPIRE_TIME = 300;
        
        /** 长期缓存过期时间（秒） */
        public static final long LONG_EXPIRE_TIME = 86400;
    }

    /**
     * WebSocket相关常量
     */
    public static class WebSocket {
        /** 连接端点 */
        public static final String ENDPOINT = "/ws";
        
        /** 原生连接端点 */
        public static final String NATIVE_ENDPOINT = "/ws-native";
        
        /** 应用目的地前缀 */
        public static final String APP_PREFIX = "/app";
        
        /** 主题前缀 */
        public static final String TOPIC_PREFIX = "/topic";
        
        /** 队列前缀 */
        public static final String QUEUE_PREFIX = "/queue";
        
        /** 用户前缀 */
        public static final String USER_PREFIX = "/user";
        
        /** 心跳间隔（秒） */
        public static final int HEARTBEAT_INTERVAL = 30;
        
        /** 连接超时时间（秒） */
        public static final int CONNECTION_TIMEOUT = 60;
    }

    /**
     * 日期时间相关常量
     */
    public static class DateTime {
        /** 标准日期时间格式 */
        public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";
        
        /** 日期格式 */
        public static final String DATE_FORMAT = "yyyy-MM-dd";
        
        /** 时间格式 */
        public static final String TIME_FORMAT = "HH:mm:ss";
        
        /** ISO日期时间格式 */
        public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        
        /** 文件名日期格式 */
        public static final String FILENAME_DATE_FORMAT = "yyyyMMdd";
        
        /** 文件名时间格式 */
        public static final String FILENAME_TIME_FORMAT = "HHmmss";
    }

    /**
     * 正则表达式常量
     */
    public static class Regex {
        /** 邮箱正则 */
        public static final String EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        /** 手机号正则（中国） */
        public static final String PHONE_CN = "^1[3-9]\\d{9}$";
        
        /** 用户名正则（字母数字下划线） */
        public static final String USERNAME = "^[a-zA-Z0-9_]{3,20}$";
        
        /** 密码正则（至少包含字母和数字） */
        public static final String PASSWORD = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{6,20}$";
        
        /** IP地址正则 */
        public static final String IP_ADDRESS = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        
        /** URL正则 */
        public static final String URL = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
    }

    /**
     * 业务错误码常量
     */
    public static class ErrorCode {
        /** 用户相关错误码基数 */
        public static final int USER_ERROR_BASE = 1000;
        
        /** 认证相关错误码基数 */
        public static final int AUTH_ERROR_BASE = 2000;
        
        /** 消息相关错误码基数 */
        public static final int MESSAGE_ERROR_BASE = 3000;
        
        /** 群组相关错误码基数 */
        public static final int GROUP_ERROR_BASE = 4000;
        
        /** 文件相关错误码基数 */
        public static final int FILE_ERROR_BASE = 5000;
        
        /** 系统相关错误码基数 */
        public static final int SYSTEM_ERROR_BASE = 9000;
    }

    /**
     * 常量使用说明：
     * 
     * 1. 系统常量：
     *    - 定义系统基本信息和配置
     *    - 包括系统名称、版本、编码等
     * 
     * 2. HTTP常量：
     *    - 定义HTTP相关的头信息和内容类型
     *    - 用于API接口开发
     * 
     * 3. 响应码常量：
     *    - 定义标准HTTP状态码
     *    - 统一API响应格式
     * 
     * 4. 业务常量：
     *    - 用户、消息、群组等业务相关常量
     *    - 包括状态码、类型、长度限制等
     * 
     * 5. 缓存常量：
     *    - Redis缓存键前缀和过期时间
     *    - 统一缓存管理
     * 
     * 6. 正则表达式：
     *    - 常用的数据验证正则
     *    - 邮箱、手机号、用户名等格式验证
     * 
     * 使用示例：
     * if (user.getStatus() == CommonConstants.User.STATUS_NORMAL) {
     *     // 用户状态正常
     * }
     * 
     * String cacheKey = CommonConstants.Cache.USER_INFO_PREFIX + userId;
     * redisTemplate.opsForValue().set(cacheKey, userInfo, CommonConstants.Cache.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
     */

}