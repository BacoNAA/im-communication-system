package com.im.imcommunicationsystem.user.util;

import java.util.regex.Pattern;
import java.util.Set;
import java.time.LocalDate;

/**
 * 用户验证工具类
 * 提供用户输入验证功能
 */
public class UserValidationUtils {

    // 用户ID正则表达式：3-20位字母、数字、下划线
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    // 昵称正则表达式：1-50位字符，不包含特殊符号
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[\u4e00-\u9fa5a-zA-Z0-9_\s]{1,50}$");
    
    // 邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // 手机号正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 性别枚举
    private static final Set<String> VALID_GENDERS = Set.of("男", "女", "保密");
    
    // 地址正则表达式：1-100位字符，不包含特殊符号
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[\u4e00-\u9fa5a-zA-Z0-9_\s]{1,100}$");
    
    // 职业正则表达式：1-100位字符，不包含特殊符号
    private static final Pattern OCCUPATION_PATTERN = Pattern.compile("^[\u4e00-\u9fa5a-zA-Z0-9_\s]{1,100}$");

    /**
     * 验证用户ID格式
     * 
     * @param userIdString 用户ID字符串
     * @return 是否有效
     */
    public static boolean isValidUserId(String userIdString) {
        if (userIdString == null || userIdString.trim().isEmpty()) {
            return false;
        }
        return USER_ID_PATTERN.matcher(userIdString.trim()).matches();
    }

    /**
     * 验证昵称格式
     * 
     * @param nickname 昵称
     * @return 是否有效
     */
    public static boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        return NICKNAME_PATTERN.matcher(nickname.trim()).matches();
    }

    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 验证个性签名长度
     * 
     * @param signature 个性签名
     * @return 是否有效
     */
    public static boolean isValidSignature(String signature) {
        if (signature == null) {
            return true; // 允许为空
        }
        return signature.length() <= 200;
    }

    /**
     * 验证状态文本长度
     * 
     * @param statusText 状态文本
     * @return 是否有效
     */
    public static boolean isValidStatusText(String statusText) {
        if (statusText == null) {
            return true; // 允许为空
        }
        return statusText.length() <= 100;
    }

    /**
     * 验证URL格式
     * 
     * @param url URL字符串
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // 允许为空
        }
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理和标准化用户输入
     * 
     * @param input 用户输入
     * @return 清理后的输入
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[\r\n\t]", " ");
    }

    /**
     * 验证字符串长度范围
     * 
     * @param input 输入字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否在有效范围内
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        int length = input.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 检查字符串是否包含敏感词
     * 
     * @param input 输入字符串
     * @return 是否包含敏感词
     */
    public static boolean containsSensitiveWords(String input) {
        // TODO: 实现敏感词检测逻辑
        if (input == null) {
            return false;
        }
        // 简单示例，实际应该使用敏感词库
        String[] sensitiveWords = {"admin", "root", "system"};
        String lowerInput = input.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerInput.contains(word)) {
                return true;
            }
        }
        return false;
    }

    // 方法别名，保持向后兼容
    public static boolean validateUserIdStr(String userIdStr) {
        return isValidUserId(userIdStr);
    }

    public static boolean validateNickname(String nickname) {
        return isValidNickname(nickname);
    }

    public static boolean validateSignature(String signature) {
        return isValidSignature(signature);
    }

    public static boolean validateStatusText(String statusText) {
        return isValidStatusText(statusText);
    }

    /**
     * 验证手机号格式
     * 
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    public static boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // 允许为空
        }
        return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /**
     * 验证性别格式
     * 
     * @param gender 性别
     * @return 是否有效
     */
    public static boolean validateGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return true; // 允许为空
        }
        return VALID_GENDERS.contains(gender.trim());
    }

    /**
     * 验证生日格式
     * 
     * @param birthday 生日
     * @return 是否有效
     */
    public static boolean validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            return true; // 允许为空
        }
        LocalDate now = LocalDate.now();
        LocalDate minDate = now.minusYears(150); // 最大150岁
        return birthday.isAfter(minDate) && birthday.isBefore(now);
    }

    /**
     * 验证所在地格式
     * 
     * @param location 所在地
     * @return 是否有效
     */
    public static boolean validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return true; // 允许为空
        }
        return LOCATION_PATTERN.matcher(location.trim()).matches();
    }

    /**
     * 验证职业格式
     * 
     * @param occupation 职业
     * @return 是否有效
     */
    public static boolean validateOccupation(String occupation) {
        if (occupation == null || occupation.trim().isEmpty()) {
            return true; // 允许为空
        }
        return OCCUPATION_PATTERN.matcher(occupation.trim()).matches();
    }
}