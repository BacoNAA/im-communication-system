package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.auth.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和验证JWT令牌
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 获取JWT密钥
     */
    private String getSecret() {
        return jwtConfig.getSecret();
    }

    /**
     * 获取访问令牌过期时间（毫秒）
     */
    private Long getAccessTokenExpiration() {
        return jwtConfig.getAccessTokenExpiration() * 1000; // 转换为毫秒
    }

    /**
     * 获取刷新令牌过期时间（毫秒）
     */
    private Long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration() * 1000; // 转换为毫秒
    }

    /**
     * 获取JWT发行者
     */
    private String getIssuer() {
        return jwtConfig.getIssuer();
    }

    /**
     * 获取密钥
     * 
     * @return SecretKey实例
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param roles 用户角色
     * @return JWT令牌
     */
    public String generateAccessToken(Long userId, String username, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("type", "access");
        
        return createToken(claims, username, getAccessTokenExpiration());
    }

    /**
     * 生成刷新令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh");
        
        return createToken(claims, username, getRefreshTokenExpiration());
    }

    /**
     * 创建令牌
     * 
     * @param claims 声明
     * @param subject 主题
     * @param expiration 过期时间
     * @return JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从令牌中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从令牌中获取用户角色
     * 
     * @param token JWT令牌
     * @return 用户角色
     */
    public String getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("roles", String.class);
    }

    /**
     * 从令牌中获取令牌类型
     * 
     * @param token JWT令牌
     * @return 令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 从令牌中获取过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取指定声明
     * 
     * @param token JWT令牌
     * @param claimsResolver 声明解析器
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.resolve(claims);
    }

    /**
     * 从令牌中获取所有声明
     * 
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("解析JWT令牌失败: {}", e.getMessage());
            throw new RuntimeException("无效的JWT令牌", e);
        }
    }

    /**
     * 检查令牌是否过期
     * 
     * @param token JWT令牌
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证令牌
     * 
     * @param token JWT令牌
     * @param username 用户名
     * @return 是否有效
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("验证JWT令牌失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证访问令牌
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public Boolean validateAccessToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return "access".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("验证访问令牌失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证刷新令牌
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public Boolean validateRefreshToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return "refresh".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("验证刷新令牌失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求头中提取令牌
     * 
     * @param authHeader 授权头
     * @return JWT令牌
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取令牌剩余有效时间（秒）
     * 
     * @param token JWT令牌
     * @return 剩余有效时间
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            return Math.max(0, (expiration.getTime() - now.getTime()) / 1000);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 获取访问令牌过期时间（毫秒）
     * 
     * @return 过期时间（毫秒）
     */
    public Long getAccessTokenExpirationTime() {
        return getAccessTokenExpiration();
    }

    /**
     * 声明解析器接口
     * 
     * @param <T> 返回类型
     */
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }

    /**
     * JWT使用说明：
     * 
     * 1. 令牌类型：
     *    - Access Token: 用于API访问，有效期24小时
     *    - Refresh Token: 用于刷新访问令牌，有效期7天
     * 
     * 2. 令牌结构：
     *    - Header: 算法和令牌类型
     *    - Payload: 用户信息和声明
     *    - Signature: 签名验证
     * 
     * 3. 声明字段：
     *    - userId: 用户ID
     *    - username: 用户名
     *    - roles: 用户角色（逗号分隔）
     *    - type: 令牌类型（access/refresh）
     *    - iss: 发行者
     *    - sub: 主题（用户名）
     *    - iat: 发行时间
     *    - exp: 过期时间
     * 
     * 4. 安全特性：
     *    - 使用HS512算法签名
     *    - 密钥长度至少256位
     *    - 支持令牌过期检查
     *    - 支持令牌类型验证
     * 
     * 5. 使用流程：
     *    - 用户登录成功后生成访问令牌和刷新令牌
     *    - 客户端在请求头中携带访问令牌
     *    - 服务器验证令牌有效性
     *    - 访问令牌过期时使用刷新令牌获取新的访问令牌
     * 
     * 6. 错误处理：
     *    - 令牌格式错误
     *    - 令牌签名无效
     *    - 令牌已过期
     *    - 令牌类型不匹配
     */

}