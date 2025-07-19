package com.im.imcommunicationsystem.group.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 邀请码工具类
 * 提供邀请码生成和验证相关的工具方法
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public class InviteCodeUtils {

    /**
     * 邀请码长度
     */
    private static final int INVITE_CODE_LENGTH = 8;
    
    /**
     * 邀请码字符集
     */
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 默认邀请码有效期（分钟）
     */
    public static final int DEFAULT_EXPIRY_MINUTES = 1440; // 24小时
    
    /**
     * 默认最大使用次数
     */
    public static final int DEFAULT_MAX_USES = 10;

    /**
     * 生成邀请码
     * 
     * @return 邀请码
     */
    public static String generateInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(INVITE_CODE_CHARS.length());
            code.append(INVITE_CODE_CHARS.charAt(randomIndex));
        }
        
        return code.toString();
    }

    /**
     * 验证邀请码格式
     * 
     * @param inviteCode 邀请码
     * @return 是否有效
     */
    public static boolean validateInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.length() != INVITE_CODE_LENGTH) {
            return false;
        }
        
        // 检查是否只包含允许的字符
        for (char c : inviteCode.toCharArray()) {
            if (INVITE_CODE_CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 生成邀请链接
     * 
     * @param baseUrl 基础URL
     * @param inviteCode 邀请码
     * @return 邀请链接
     */
    public static String generateInviteUrl(String baseUrl, String inviteCode) {
        if (baseUrl == null || baseUrl.isEmpty() || inviteCode == null || inviteCode.isEmpty()) {
            return null;
        }
        
        // 确保基础URL以/结尾
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        
        return url + "join?code=" + inviteCode;
    }

    /**
     * 从邀请链接中解析邀请码
     * 
     * @param inviteUrl 邀请链接
     * @return 邀请码，无效返回null
     */
    public static String parseInviteUrl(String inviteUrl) {
        if (inviteUrl == null || inviteUrl.isEmpty()) {
            return null;
        }
        
        // 查找code参数
        int codeIndex = inviteUrl.indexOf("code=");
        if (codeIndex == -1) {
            return null;
        }
        
        // 提取code值
        String code = inviteUrl.substring(codeIndex + 5);
        
        // 如果有其他参数，截取到&之前
        int andIndex = code.indexOf("&");
        if (andIndex != -1) {
            code = code.substring(0, andIndex);
        }
        
        return validateInviteCode(code) ? code : null;
    }

    /**
     * 生成二维码内容
     * 
     * @param groupId 群组ID
     * @param inviteCode 邀请码
     * @return 二维码内容
     */
    public static String generateQRCodeContent(Long groupId, String inviteCode) {
        if (groupId == null || inviteCode == null || inviteCode.isEmpty()) {
            return null;
        }
        
        // 创建包含群组ID和邀请码的JSON格式字符串
        String content = String.format("{\"groupId\":%d,\"inviteCode\":\"%s\"}", groupId, inviteCode);
        
        // Base64编码
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    /**
     * 计算邀请码过期时间
     * 
     * @param expiryMinutes 过期分钟数，null表示使用默认值
     * @return 过期时间
     */
    public static LocalDateTime calculateExpiryTime(Integer expiryMinutes) {
        int minutes = expiryMinutes != null ? expiryMinutes : DEFAULT_EXPIRY_MINUTES;
        return LocalDateTime.now().plusMinutes(minutes);
    }
} 