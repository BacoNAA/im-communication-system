package com.im.imcommunicationsystem.relationship.utils;

import com.im.imcommunicationsystem.relationship.constants.RelationshipConstants;
import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactRequest;
import com.im.imcommunicationsystem.relationship.entity.ContactTag;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 联系人关系工具类
 * 
 * @author System
 * @since 1.0.0
 */
@Slf4j
public class RelationshipUtils {

    /**
     * 标签名称验证正则
     */
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile(RelationshipConstants.Regex.TAG_NAME_PATTERN);

    /**
     * 颜色代码验证正则
     */
    private static final Pattern COLOR_PATTERN = Pattern.compile(RelationshipConstants.Regex.COLOR_PATTERN);

    /**
     * 备注验证正则
     */
    private static final Pattern REMARK_PATTERN = Pattern.compile(RelationshipConstants.Regex.REMARK_PATTERN);

    /**
     * 验证标签名称是否有效
     *
     * @param tagName 标签名称
     * @return 是否有效
     */
    public static boolean isValidTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return false;
        }
        
        String trimmedName = tagName.trim();
        return trimmedName.length() >= RelationshipConstants.Tag.MIN_NAME_LENGTH
                && trimmedName.length() <= RelationshipConstants.Tag.MAX_NAME_LENGTH
                && TAG_NAME_PATTERN.matcher(trimmedName).matches();
    }

    /**
     * 验证颜色代码是否有效
     *
     * @param color 颜色代码
     * @return 是否有效
     */
    public static boolean isValidColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return false;
        }
        return COLOR_PATTERN.matcher(color.trim()).matches();
    }

    /**
     * 验证备注是否有效
     *
     * @param remark 备注
     * @return 是否有效
     */
    public static boolean isValidRemark(String remark) {
        if (remark == null) {
            return true; // 备注可以为空
        }
        return remark.length() <= RelationshipConstants.Contact.MAX_REMARK_LENGTH
                && REMARK_PATTERN.matcher(remark).matches();
    }

    /**
     * 验证好友请求消息是否有效
     *
     * @param message 请求消息
     * @return 是否有效
     */
    public static boolean isValidRequestMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true; // 消息可以为空，会使用默认消息
        }
        return message.trim().length() <= RelationshipConstants.Request.MAX_MESSAGE_LENGTH;
    }

    /**
     * 生成缓存键
     *
     * @param prefix 前缀
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String generateCacheKey(String prefix, Long userId) {
        return prefix + userId;
    }

    /**
     * 生成缓存键
     *
     * @param prefix 前缀
     * @param userId 用户ID
     * @param suffix 后缀
     * @return 缓存键
     */
    public static String generateCacheKey(String prefix, Long userId, String suffix) {
        return prefix + userId + ":" + suffix;
    }

    /**
     * 检查好友请求是否过期
     *
     * @param request 好友请求
     * @return 是否过期
     */
    public static boolean isRequestExpired(ContactRequest request) {
        if (request == null || request.getCreatedAt() == null) {
            return true;
        }
        
        LocalDateTime expiryTime = request.getCreatedAt()
                .plusDays(RelationshipConstants.Request.EXPIRY_DAYS);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 检查好友请求是否可以处理
     *
     * @param request 好友请求
     * @return 是否可以处理
     */
    public static boolean canHandleRequest(ContactRequest request) {
        if (request == null) {
            return false;
        }
        
        return ContactRequestStatus.PENDING.equals(request.getStatus()) 
                && !isRequestExpired(request);
    }

    /**
     * 过滤有效的标签
     *
     * @param tags 标签列表
     * @return 有效的标签列表
     */
    public static List<ContactTag> filterValidTags(List<ContactTag> tags) {
        if (tags == null) {
            return List.of();
        }
        
        return tags.stream()
                .filter(tag -> tag != null && isValidTagName(tag.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 过滤活跃的联系人
     *
     * @param contacts 联系人列表
     * @return 活跃的联系人列表
     */
    public static List<Contact> filterActiveContacts(List<Contact> contacts) {
        if (contacts == null) {
            return List.of();
        }
        
        return contacts.stream()
                .filter(contact -> contact != null && !contact.getIsBlocked())
                .collect(Collectors.toList());
    }

    /**
     * 过滤待处理的好友请求
     *
     * @param requests 好友请求列表
     * @return 待处理的好友请求列表
     */
    public static List<ContactRequest> filterPendingRequests(List<ContactRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        
        return requests.stream()
                .filter(RelationshipUtils::canHandleRequest)
                .collect(Collectors.toList());
    }

    /**
     * 获取默认标签颜色
     *
     * @param index 索引
     * @return 颜色代码
     */
    public static String getDefaultColor(int index) {
        String[] colors = RelationshipConstants.Tag.DEFAULT_COLORS;
        if (index < 0 || index >= colors.length) {
            return colors[0]; // 返回第一个颜色作为默认值
        }
        return colors[index];
    }

    /**
     * 获取随机默认颜色
     *
     * @return 颜色代码
     */
    public static String getRandomDefaultColor() {
        String[] colors = RelationshipConstants.Tag.DEFAULT_COLORS;
        int randomIndex = (int) (Math.random() * colors.length);
        return colors[randomIndex];
    }

    /**
     * 格式化备注
     *
     * @param remark 原始备注
     * @return 格式化后的备注
     */
    public static String formatRemark(String remark) {
        if (remark == null) {
            return RelationshipConstants.Contact.DEFAULT_REMARK;
        }
        
        String trimmed = remark.trim();
        if (trimmed.isEmpty()) {
            return RelationshipConstants.Contact.DEFAULT_REMARK;
        }
        
        // 限制长度
        if (trimmed.length() > RelationshipConstants.Contact.MAX_REMARK_LENGTH) {
            trimmed = trimmed.substring(0, RelationshipConstants.Contact.MAX_REMARK_LENGTH);
        }
        
        return trimmed;
    }

    /**
     * 格式化请求消息
     *
     * @param message 原始消息
     * @return 格式化后的消息
     */
    public static String formatRequestMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return RelationshipConstants.Request.DEFAULT_MESSAGE;
        }
        
        String trimmed = message.trim();
        
        // 限制长度
        if (trimmed.length() > RelationshipConstants.Request.MAX_MESSAGE_LENGTH) {
            trimmed = trimmed.substring(0, RelationshipConstants.Request.MAX_MESSAGE_LENGTH);
        }
        
        return trimmed;
    }

    /**
     * 检查是否可以添加更多好友
     *
     * @param currentFriendsCount 当前好友数量
     * @return 是否可以添加
     */
    public static boolean canAddMoreFriends(int currentFriendsCount) {
        return currentFriendsCount < RelationshipConstants.Contact.MAX_FRIENDS_COUNT;
    }

    /**
     * 检查是否可以创建更多标签
     *
     * @param currentTagsCount 当前标签数量
     * @return 是否可以创建
     */
    public static boolean canCreateMoreTags(int currentTagsCount) {
        return currentTagsCount < RelationshipConstants.Tag.MAX_TAGS_PER_USER;
    }

    /**
     * 计算好友请求剩余有效时间（小时）
     *
     * @param request 好友请求
     * @return 剩余小时数，如果已过期返回0
     */
    public static long getRequestRemainingHours(ContactRequest request) {
        if (request == null || request.getCreatedAt() == null) {
            return 0;
        }
        
        LocalDateTime expiryTime = request.getCreatedAt()
                .plusDays(RelationshipConstants.Request.EXPIRY_DAYS);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(expiryTime)) {
            return 0;
        }
        
        return java.time.Duration.between(now, expiryTime).toHours();
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RelationshipUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}