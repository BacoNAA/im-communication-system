package com.im.imcommunicationsystem.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置类
 */
@Configuration
@ConfigurationProperties(prefix = "app.file-upload")
public class FileUploadConfig {

    private String uploadPath = "/uploads";
    private String baseUrl = "http://localhost:8080";
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private long maxRequestSize = 50 * 1024 * 1024; // 50MB
    private String[] allowedImageTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    private String[] allowedFileTypes = {"image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"};
    
    // 图片压缩配置
    private ImageCompressionConfig imageCompression = new ImageCompressionConfig();
    
    // 头像配置
    private AvatarConfig avatar = new AvatarConfig();

    // Getter和Setter方法
    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public String[] getAllowedImageTypes() {
        return allowedImageTypes;
    }

    public void setAllowedImageTypes(String[] allowedImageTypes) {
        this.allowedImageTypes = allowedImageTypes;
    }

    public String[] getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(String[] allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }

    public ImageCompressionConfig getImageCompression() {
        return imageCompression;
    }

    public void setImageCompression(ImageCompressionConfig imageCompression) {
        this.imageCompression = imageCompression;
    }

    public AvatarConfig getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarConfig avatar) {
        this.avatar = avatar;
    }

    /**
     * 图片压缩配置
     */
    public static class ImageCompressionConfig {
        private boolean enabled = true;
        private float quality = 0.8f;
        private int maxWidth = 1920;
        private int maxHeight = 1080;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public float getQuality() {
            return quality;
        }

        public void setQuality(float quality) {
            this.quality = quality;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }
    }

    /**
     * 头像配置
     */
    public static class AvatarConfig {
        private int size = 200; // 头像尺寸
        private String defaultAvatar = "/images/default-avatar.png";
        private String directory = "avatars";

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getDefaultAvatar() {
            return defaultAvatar;
        }

        public void setDefaultAvatar(String defaultAvatar) {
            this.defaultAvatar = defaultAvatar;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }
}