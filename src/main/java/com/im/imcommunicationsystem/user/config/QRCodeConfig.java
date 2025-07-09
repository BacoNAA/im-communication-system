package com.im.imcommunicationsystem.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 二维码配置类
 */
@Configuration
@ConfigurationProperties(prefix = "app.qrcode")
public class QRCodeConfig {

    private int width = 300;
    private int height = 300;
    private String format = "PNG";
    private String charset = "UTF-8";
    private int margin = 1;
    private String errorCorrectionLevel = "M"; // L, M, Q, H
    private String directory = "qrcodes";
    private long expirationHours = 24; // 二维码有效期（小时）
    
    // 用户信息编码配置
    private UserInfoConfig userInfo = new UserInfoConfig();
    
    // 样式配置
    private StyleConfig style = new StyleConfig();

    // Getter和Setter方法
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public String getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    public void setErrorCorrectionLevel(String errorCorrectionLevel) {
        this.errorCorrectionLevel = errorCorrectionLevel;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public long getExpirationHours() {
        return expirationHours;
    }

    public void setExpirationHours(long expirationHours) {
        this.expirationHours = expirationHours;
    }

    public UserInfoConfig getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoConfig userInfo) {
        this.userInfo = userInfo;
    }

    public StyleConfig getStyle() {
        return style;
    }

    public void setStyle(StyleConfig style) {
        this.style = style;
    }

    /**
     * 用户信息编码配置
     */
    public static class UserInfoConfig {
        private String prefix = "IM_USER:";
        private boolean includeAvatar = true;
        private boolean includeSignature = false;
        private String encryptionKey = "default_key";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public boolean isIncludeAvatar() {
            return includeAvatar;
        }

        public void setIncludeAvatar(boolean includeAvatar) {
            this.includeAvatar = includeAvatar;
        }

        public boolean isIncludeSignature() {
            return includeSignature;
        }

        public void setIncludeSignature(boolean includeSignature) {
            this.includeSignature = includeSignature;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }
    }

    /**
     * 二维码样式配置
     */
    public static class StyleConfig {
        private String foregroundColor = "#000000";
        private String backgroundColor = "#FFFFFF";
        private boolean addLogo = false;
        private String logoPath = "/images/logo.png";
        private int logoSize = 60;

        public String getForegroundColor() {
            return foregroundColor;
        }

        public void setForegroundColor(String foregroundColor) {
            this.foregroundColor = foregroundColor;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public boolean isAddLogo() {
            return addLogo;
        }

        public void setAddLogo(boolean addLogo) {
            this.addLogo = addLogo;
        }

        public String getLogoPath() {
            return logoPath;
        }

        public void setLogoPath(String logoPath) {
            this.logoPath = logoPath;
        }

        public int getLogoSize() {
            return logoSize;
        }

        public void setLogoSize(int logoSize) {
            this.logoSize = logoSize;
        }
    }
}