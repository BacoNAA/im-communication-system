package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 数据验证工具类
 * 提供统一的数据验证功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class ValidationUtils {

    /**
     * 编译后的正则表达式模式（提高性能）
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(CommonConstants.Regex.EMAIL);
    private static final Pattern PHONE_CN_PATTERN = Pattern.compile(CommonConstants.Regex.PHONE_CN);
    private static final Pattern USERNAME_PATTERN = Pattern.compile(CommonConstants.Regex.USERNAME);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(CommonConstants.Regex.PASSWORD);
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(CommonConstants.Regex.IP_ADDRESS);
    private static final Pattern URL_PATTERN = Pattern.compile(CommonConstants.Regex.URL);

    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 验证中国手机号格式
     * 
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhoneCN(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_CN_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * 验证用户名格式
     * 
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String trimmed = username.trim();
        return trimmed.length() >= CommonConstants.User.USERNAME_MIN_LENGTH &&
               trimmed.length() <= CommonConstants.User.USERNAME_MAX_LENGTH &&
               USERNAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 验证密码格式
     * 
     * @param password 密码
     * @return 是否有效
     */
    public static boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        return password.length() >= CommonConstants.User.PASSWORD_MIN_LENGTH &&
               password.length() <= CommonConstants.User.PASSWORD_MAX_LENGTH &&
               PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证IP地址格式
     * 
     * @param ip IP地址
     * @return 是否有效
     */
    public static boolean isValidIpAddress(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        return IP_ADDRESS_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * 验证URL格式
     * 
     * @param url URL地址
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * 验证昵称
     * 
     * @param nickname 昵称
     * @return 是否有效
     */
    public static boolean isValidNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return false;
        }
        String trimmed = nickname.trim();
        return trimmed.length() > 0 && trimmed.length() <= CommonConstants.User.NICKNAME_MAX_LENGTH;
    }

    /**
     * 验证个性签名
     * 
     * @param signature 个性签名
     * @return 是否有效
     */
    public static boolean isValidSignature(String signature) {
        if (signature == null) {
            return true; // 个性签名可以为空
        }
        return signature.length() <= CommonConstants.User.SIGNATURE_MAX_LENGTH;
    }

    /**
     * 验证群组名称
     * 
     * @param groupName 群组名称
     * @return 是否有效
     */
    public static boolean isValidGroupName(String groupName) {
        if (!StringUtils.hasText(groupName)) {
            return false;
        }
        String trimmed = groupName.trim();
        return trimmed.length() > 0 && trimmed.length() <= CommonConstants.Group.NAME_MAX_LENGTH;
    }

    /**
     * 验证群组描述
     * 
     * @param description 群组描述
     * @return 是否有效
     */
    public static boolean isValidGroupDescription(String description) {
        if (description == null) {
            return true; // 群组描述可以为空
        }
        return description.length() <= CommonConstants.Group.DESCRIPTION_MAX_LENGTH;
    }

    /**
     * 验证消息内容
     * 
     * @param content 消息内容
     * @return 是否有效
     */
    public static boolean isValidMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        return content.trim().length() > 0 && content.length() <= CommonConstants.Message.TEXT_MAX_LENGTH;
    }

    /**
     * 验证文件名
     * 
     * @param filename 文件名
     * @return 是否有效
     */
    public static boolean isValidFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        String trimmed = filename.trim();
        // 检查长度
        if (trimmed.length() > CommonConstants.File.FILENAME_MAX_LENGTH) {
            return false;
        }
        // 检查是否包含非法字符
        String[] illegalChars = {"/", "\\", ":", "*", "?", "\"", "<", ">", "|"};
        for (String illegalChar : illegalChars) {
            if (trimmed.contains(illegalChar)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证ID是否有效（正整数）
     * 
     * @param id ID值
     * @return 是否有效
     */
    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    /**
     * 验证页码
     * 
     * @param page 页码
     * @return 是否有效
     */
    public static boolean isValidPage(Integer page) {
        return page != null && page > 0;
    }

    /**
     * 验证页面大小
     * 
     * @param size 页面大小
     * @return 是否有效
     */
    public static boolean isValidPageSize(Integer size) {
        return size != null && size > 0 && size <= 100; // 限制最大100条
    }

    /**
     * 验证验证码
     * 
     * @param code 验证码
     * @param expectedLength 期望长度
     * @return 是否有效
     */
    public static boolean isValidVerificationCode(String code, int expectedLength) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        String trimmed = code.trim();
        return trimmed.length() == expectedLength && trimmed.matches("\\d+");
    }

    /**
     * 验证字符串长度
     * 
     * @param str 字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否有效
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength == 0;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 验证字符串是否为空或仅包含空白字符
     * 
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * 验证字符串是否不为空
     * 
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return StringUtils.hasText(str);
    }

    /**
     * 验证数值范围
     * 
     * @param value 数值
     * @param min 最小值
     * @param max 最大值
     * @return 是否在范围内
     */
    public static boolean isInRange(Number value, Number min, Number max) {
        if (value == null) {
            return false;
        }
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        return val >= minVal && val <= maxVal;
    }

    /**
     * 验证枚举值
     * 
     * @param value 值
     * @param enumClass 枚举类
     * @return 是否有效
     */
    public static <E extends Enum<E>> boolean isValidEnum(String value, Class<E> enumClass) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            Enum.valueOf(enumClass, value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证数组是否包含指定值
     * 
     * @param value 值
     * @param allowedValues 允许的值数组
     * @return 是否包含
     */
    public static boolean isInArray(String value, String[] allowedValues) {
        if (!StringUtils.hasText(value) || allowedValues == null) {
            return false;
        }
        for (String allowedValue : allowedValues) {
            if (value.equals(allowedValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理和标准化字符串
     * 
     * @param str 原始字符串
     * @return 清理后的字符串
     */
    public static String sanitizeString(String str) {
        if (str == null) {
            return null;
        }
        // 去除首尾空白字符
        String cleaned = str.trim();
        // 移除控制字符
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        // 压缩多个连续空格为单个空格
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    /**
     * 验证工具使用说明：
     * 
     * 1. 基础验证：
     *    - 邮箱、手机号、用户名、密码格式验证
     *    - IP地址、URL格式验证
     *    - 字符串长度和空值验证
     * 
     * 2. 业务验证：
     *    - 用户信息验证（昵称、签名等）
     *    - 群组信息验证（名称、描述等）
     *    - 消息内容验证
     *    - 文件名验证
     * 
     * 3. 数值验证：
     *    - ID有效性验证
     *    - 分页参数验证
     *    - 数值范围验证
     * 
     * 4. 枚举和数组验证：
     *    - 枚举值有效性验证
     *    - 数组包含验证
     * 
     * 5. 字符串处理：
     *    - 字符串清理和标准化
     *    - 移除控制字符和多余空格
     * 
     * 使用示例：
     * 
     * // 验证邮箱
     * if (!ValidationUtils.isValidEmail(email)) {
     *     throw new BusinessException("邮箱格式不正确");
     * }
     * 
     * // 验证用户名
     * if (!ValidationUtils.isValidUsername(username)) {
     *     throw new BusinessException("用户名格式不正确");
     * }
     * 
     * // 验证分页参数
     * if (!ValidationUtils.isValidPage(page) || !ValidationUtils.isValidPageSize(size)) {
     *     throw new BusinessException("分页参数不正确");
     * }
     * 
     * // 清理字符串
     * String cleanedInput = ValidationUtils.sanitizeString(userInput);
     * 
     * 注意事项：
     * 1. 所有验证方法都是静态方法，可以直接调用
     * 2. 验证失败时返回false，不抛出异常
     * 3. 空值和null值的处理根据业务需求而定
     * 4. 正则表达式已预编译，提高性能
     * 5. 建议在Controller层进行参数验证
     */

}