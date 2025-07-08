package com.im.imcommunicationsystem.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 * 提供密码加密、数据加密等安全功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class EncryptionUtils {

    /**
     * 加密算法常量
     */
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String MD5_ALGORITHM = "MD5";
    
    /**
     * AES-GCM参数
     */
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 默认盐值长度
     */
    private static final int DEFAULT_SALT_LENGTH = 16;

    /**
     * 生成MD5哈希
     * 
     * @param input 输入字符串
     * @return MD5哈希值（小写十六进制）
     */
    public static String md5(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 生成SHA-256哈希
     * 
     * @param input 输入字符串
     * @return SHA-256哈希值（小写十六进制）
     */
    public static String sha256(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            throw new RuntimeException("SHA-256加密失败", e);
        }
    }

    /**
     * 生成带盐值的SHA-256哈希（用于密码加密）
     * 
     * @param password 密码
     * @param salt 盐值
     * @return 哈希值
     */
    public static String sha256WithSalt(String password, String salt) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(salt)) {
            return "";
        }
        return sha256(password + salt);
    }

    /**
     * 生成随机盐值
     * 
     * @return Base64编码的盐值
     */
    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }

    /**
     * 生成指定长度的随机盐值
     * 
     * @param length 盐值长度（字节）
     * @return Base64编码的盐值
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 密码加密（生成盐值并加密）
     * 
     * @param password 原始密码
     * @return 加密结果（格式：盐值$哈希值）
     */
    public static String encryptPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return "";
        }
        String salt = generateSalt();
        String hash = sha256WithSalt(password, salt);
        return salt + "$" + hash;
    }

    /**
     * 验证密码
     * 
     * @param password 原始密码
     * @param encryptedPassword 加密后的密码（格式：盐值$哈希值）
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String encryptedPassword) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(encryptedPassword)) {
            return false;
        }
        
        String[] parts = encryptedPassword.split("\\$");
        if (parts.length != 2) {
            return false;
        }
        
        String salt = parts[0];
        String expectedHash = parts[1];
        String actualHash = sha256WithSalt(password, salt);
        
        return expectedHash.equals(actualHash);
    }

    /**
     * 生成AES密钥
     * 
     * @return Base64编码的密钥
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(256); // 256位密钥
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("AES密钥生成失败", e);
            throw new RuntimeException("AES密钥生成失败", e);
        }
    }

    /**
     * AES加密
     * 
     * @param plaintext 明文
     * @param key Base64编码的密钥
     * @return Base64编码的密文（包含IV）
     */
    public static String aesEncrypt(String plaintext, String key) {
        if (!StringUtils.hasText(plaintext) || !StringUtils.hasText(key)) {
            return "";
        }
        
        try {
            // 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 组合IV和密文
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES解密
     * 
     * @param ciphertext Base64编码的密文（包含IV）
     * @param key Base64编码的密钥
     * @return 明文
     */
    public static String aesDecrypt(String ciphertext, String key) {
        if (!StringUtils.hasText(ciphertext) || !StringUtils.hasText(key)) {
            return "";
        }
        
        try {
            // 解码密钥和密文
            byte[] keyBytes = Base64.getDecoder().decode(key);
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);
            
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足");
            }
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] actualCiphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, actualCiphertext, 0, actualCiphertext.length);
            
            // 初始化解密器
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // 解密
            byte[] plaintext = cipher.doFinal(actualCiphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 生成随机字符串（用于令牌、验证码等）
     * 
     * @param length 长度
     * @param includeNumbers 是否包含数字
     * @param includeLetters 是否包含字母
     * @param includeSymbols 是否包含符号
     * @return 随机字符串
     */
    public static String generateRandomString(int length, boolean includeNumbers, 
                                            boolean includeLetters, boolean includeSymbols) {
        if (length <= 0) {
            return "";
        }
        
        StringBuilder chars = new StringBuilder();
        if (includeNumbers) {
            chars.append("0123456789");
        }
        if (includeLetters) {
            chars.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        if (includeSymbols) {
            chars.append("!@#$%^&*()_+-=[]{}|;:,.<>?");
        }
        
        if (chars.length() == 0) {
            throw new IllegalArgumentException("至少需要包含一种字符类型");
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(chars.length());
            result.append(chars.charAt(index));
        }
        
        return result.toString();
    }

    /**
     * 生成数字验证码
     * 
     * @param length 长度
     * @return 数字验证码
     */
    public static String generateNumericCode(int length) {
        return generateRandomString(length, true, false, false);
    }

    /**
     * 生成字母数字验证码
     * 
     * @param length 长度
     * @return 字母数字验证码
     */
    public static String generateAlphanumericCode(int length) {
        return generateRandomString(length, true, true, false);
    }

    /**
     * 生成强密码
     * 
     * @param length 长度
     * @return 强密码
     */
    public static String generateStrongPassword(int length) {
        return generateRandomString(length, true, true, true);
    }

    /**
     * 生成UUID（去除连字符）
     * 
     * @return UUID字符串
     */
    public static String generateUUID() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Base64编码
     * 
     * @param input 输入字符串
     * @return Base64编码结果
     */
    public static String base64Encode(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64解码
     * 
     * @param input Base64编码的字符串
     * @return 解码结果
     */
    public static String base64Decode(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Base64解码失败: {}", input, e);
            return "";
        }
    }

    /**
     * URL安全的Base64编码
     * 
     * @param input 输入字符串
     * @return URL安全的Base64编码结果
     */
    public static String base64UrlEncode(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL安全的Base64解码
     * 
     * @param input URL安全的Base64编码字符串
     * @return 解码结果
     */
    public static String base64UrlDecode(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("URL安全Base64解码失败: {}", input, e);
            return "";
        }
    }

    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 十六进制字符串转字节数组
     * 
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (!StringUtils.hasText(hex) || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("无效的十六进制字符串");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    /**
     * 简单的字符串混淆（用于非敏感数据）
     * 
     * @param input 输入字符串
     * @param key 混淆密钥
     * @return 混淆后的字符串
     */
    public static String simpleObfuscate(String input, String key) {
        if (!StringUtils.hasText(input) || !StringUtils.hasText(key)) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            char k = key.charAt(i % key.length());
            result.append((char) (c ^ k));
        }
        
        return Base64.getEncoder().encodeToString(result.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 简单的字符串反混淆
     * 
     * @param obfuscated 混淆后的字符串
     * @param key 混淆密钥
     * @return 原始字符串
     */
    public static String simpleDeobfuscate(String obfuscated, String key) {
        if (!StringUtils.hasText(obfuscated) || !StringUtils.hasText(key)) {
            return obfuscated;
        }
        
        try {
            String decoded = new String(Base64.getDecoder().decode(obfuscated), StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < decoded.length(); i++) {
                char c = decoded.charAt(i);
                char k = key.charAt(i % key.length());
                result.append((char) (c ^ k));
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("字符串反混淆失败", e);
            return obfuscated;
        }
    }

    /**
     * 加密工具使用说明：
     * 
     * 1. 哈希算法：
     *    - md5() - MD5哈希（不推荐用于密码）
     *    - sha256() - SHA-256哈希
     *    - sha256WithSalt() - 带盐值的SHA-256哈希
     * 
     * 2. 密码加密：
     *    - encryptPassword() - 密码加密（自动生成盐值）
     *    - verifyPassword() - 密码验证
     *    - generateSalt() - 生成盐值
     * 
     * 3. 对称加密：
     *    - generateAESKey() - 生成AES密钥
     *    - aesEncrypt() / aesDecrypt() - AES加密解密
     * 
     * 4. 随机生成：
     *    - generateRandomString() - 生成随机字符串
     *    - generateNumericCode() - 生成数字验证码
     *    - generateAlphanumericCode() - 生成字母数字验证码
     *    - generateStrongPassword() - 生成强密码
     *    - generateUUID() - 生成UUID
     * 
     * 5. 编码解码：
     *    - base64Encode() / base64Decode() - Base64编码解码
     *    - base64UrlEncode() / base64UrlDecode() - URL安全Base64编码解码
     *    - bytesToHex() / hexToBytes() - 十六进制转换
     * 
     * 6. 简单混淆：
     *    - simpleObfuscate() / simpleDeobfuscate() - 简单字符串混淆
     * 
     * 使用示例：
     * 
     * // 密码加密
     * String encryptedPassword = EncryptionUtils.encryptPassword("mypassword");
     * boolean isValid = EncryptionUtils.verifyPassword("mypassword", encryptedPassword);
     * 
     * // AES加密
     * String key = EncryptionUtils.generateAESKey();
     * String encrypted = EncryptionUtils.aesEncrypt("sensitive data", key);
     * String decrypted = EncryptionUtils.aesDecrypt(encrypted, key);
     * 
     * // 生成验证码
     * String code = EncryptionUtils.generateNumericCode(6);
     * 
     * // 文件哈希
     * String fileHash = EncryptionUtils.sha256(fileContent);
     * 
     * 安全注意事项：
     * 1. 密码存储：使用encryptPassword()，不要直接存储明文密码
     * 2. 敏感数据：使用AES加密，密钥需要安全存储
     * 3. 随机数：使用SecureRandom生成安全的随机数
     * 4. 密钥管理：AES密钥应该定期轮换
     * 5. 算法选择：优先使用SHA-256而不是MD5
     * 6. 盐值：每个密码使用不同的盐值
     */

}