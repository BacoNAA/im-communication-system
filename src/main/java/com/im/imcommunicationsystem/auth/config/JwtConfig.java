package com.im.imcommunicationsystem.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 * 配置JWT相关参数
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /**
     * JWT密钥
     */
    private String secret = "im-communication-system-jwt-secret-key-2024";

    /**
     * 访问令牌过期时间（秒）
     */
    private Long accessTokenExpiration = 3600L; // 1小时

    /**
     * 刷新令牌过期时间（秒）
     */
    private Long refreshTokenExpiration = 604800L; // 7天

    /**
     * 临时令牌过期时间（秒）
     */
    private Long tempTokenExpiration = 300L; // 5分钟

    /**
     * 令牌前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * 请求头名称
     */
    private String headerName = "Authorization";

    /**
     * JWT签发者
     */
    private String issuer = "im-communication-system";

    // Getters and Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(Long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(Long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public Long getTempTokenExpiration() {
        return tempTokenExpiration;
    }

    public void setTempTokenExpiration(Long tempTokenExpiration) {
        this.tempTokenExpiration = tempTokenExpiration;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}