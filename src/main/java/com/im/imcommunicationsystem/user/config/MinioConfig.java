package com.im.imcommunicationsystem.user.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * MinIO对象存储配置类
 * 配置MinIO客户端和相关参数
 */
@Configuration
@ConfigurationProperties(prefix = "app.minio")
@Data
@Slf4j
public class MinioConfig {

    /**
     * MinIO服务端点
     */
    private String endpoint = "http://localhost:9000";

    /**
     * 访问密钥
     */
    private String accessKey = "minioadmin";

    /**
     * 秘密密钥
     */
    private String secretKey = "minioadmin";

    // 新的存储桶配置 - 按访问级别分类
    @Value("${app.minio.public-bucket:im-public-files}")
    private String publicDefaultBucket;
    
    @Value("${app.minio.private-bucket:im-private-files}")
    private String privateDefaultBucket;
    
    // 详细的公开存储桶配置
    private String publicImageBucket = "im-public-images";
    private String publicVideoBucket = "im-public-videos";
    private String publicDocumentBucket = "im-public-documents";
    private String publicAudioBucket = "im-public-audios";
    private String publicOtherBucket = "im-public-others";
    
    // 详细的私有存储桶配置
    private String privateImageBucket = "im-private-images";
    private String privateVideoBucket = "im-private-videos";
    private String privateDocumentBucket = "im-private-documents";
    private String privateAudioBucket = "im-private-audios";
    private String privateOtherBucket = "im-private-others";
    


    /**
     * 文件URL过期时间（秒）
     */
    private int urlExpireSeconds = 7 * 24 * 60 * 60; // 7天

    /**
     * 是否启用HTTPS
     */
    private boolean secure = false;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectTimeout = 10000;

    /**
     * 写入超时时间（毫秒）
     */
    private long writeTimeout = 60000;

    /**
     * 读取超时时间（毫秒）
     */
    private long readTimeout = 10000;

    /**
     * 创建MinIO客户端Bean
     *
     * @return MinIO客户端实例
     */
    @Bean
    public MinioClient minioClient() {
        try {
            log.info("初始化MinIO客户端，端点: {}, 访问密钥: {}", endpoint, accessKey);
            
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // 设置超时时间
            client.setTimeout(connectTimeout, writeTimeout, readTimeout);
            
            log.info("MinIO客户端初始化成功");
            
            // 初始化存储桶
            initializeBuckets(client);
            
            return client;
            
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败", e);
            throw new RuntimeException("MinIO客户端初始化失败", e);
        }
    }

    /**
     * 初始化所需的存储桶
     *
     * @param client MinIO客户端
     */
    private void initializeBuckets(MinioClient client) {
        try {
            log.info("开始检查并创建MinIO存储桶...");
            
            // 初始化公开存储桶
            createBucketIfNotExists(client, publicDefaultBucket);
            createBucketIfNotExists(client, publicImageBucket);
            createBucketIfNotExists(client, publicVideoBucket);
            createBucketIfNotExists(client, publicDocumentBucket);
            createBucketIfNotExists(client, publicAudioBucket);
            createBucketIfNotExists(client, publicOtherBucket);
            
            // 初始化私有存储桶
            createBucketIfNotExists(client, privateDefaultBucket);
            createBucketIfNotExists(client, privateImageBucket);
            createBucketIfNotExists(client, privateVideoBucket);
            createBucketIfNotExists(client, privateDocumentBucket);
            createBucketIfNotExists(client, privateAudioBucket);
            createBucketIfNotExists(client, privateOtherBucket);
            
            log.info("MinIO存储桶初始化完成 - 公开存储桶: 6个, 私有存储桶: 6个");
            
        } catch (Exception e) {
            log.error("初始化存储桶时发生错误", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 创建存储桶（如果不存在）
     *
     * @param client MinIO客户端
     * @param bucketName 存储桶名称
     */
    private void createBucketIfNotExists(MinioClient client, String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            return;
        }
        
        try {
            // 检查存储桶是否存在
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );
            
            if (!exists) {
                // 创建存储桶
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("成功创建存储桶: {}", bucketName);
            } else {
                log.debug("存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("处理存储桶 {} 时发生错误: {}", bucketName, e.getMessage());
        }
    }



    /**
     * 根据文件类型和访问级别获取对应的存储桶名称
     *
     * @param fileType 文件类型
     * @param isPrivate 是否为私有文件
     * @return 存储桶名称
     */
    public String getBucketName(String fileType, boolean isPrivate) {
        if (fileType == null) {
            return isPrivate ? privateDefaultBucket : publicDefaultBucket;
        }
        
        switch (fileType.toLowerCase()) {
            case "image":
                return isPrivate ? privateImageBucket : publicImageBucket;
            case "video":
                return isPrivate ? privateVideoBucket : publicVideoBucket;
            case "audio":
                return isPrivate ? privateAudioBucket : publicAudioBucket;
            case "document":
                return isPrivate ? privateDocumentBucket : publicDocumentBucket;
            default:
                return isPrivate ? privateOtherBucket : publicOtherBucket;
        }
    }

    /**
     * 获取所有存储桶名称列表
     *
     * @return 存储桶名称数组
     */
    public String[] getAllBucketNames() {
        return new String[]{
            // 公开存储桶
            publicDefaultBucket,
            publicImageBucket,
            publicVideoBucket,
            publicDocumentBucket,
            publicAudioBucket,
            publicOtherBucket,
            // 私有存储桶
            privateDefaultBucket,
            privateImageBucket,
            privateVideoBucket,
            privateDocumentBucket,
            privateAudioBucket,
            privateOtherBucket,

        };
    }

    /**
     * 验证配置是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return endpoint != null && !endpoint.trim().isEmpty() &&
               accessKey != null && !accessKey.trim().isEmpty() &&
               secretKey != null && !secretKey.trim().isEmpty();
    }

    /**
     * 获取完整的文件访问URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey 对象键
     * @return 文件访问URL
     */
    public String getFileUrl(String bucketName, String objectKey) {
        if (bucketName == null || objectKey == null) {
            return null;
        }
        
        String baseUrl = endpoint;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        return baseUrl + bucketName + "/" + objectKey;
    }

    // Getter和Setter方法
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    // 公开存储桶 Getter/Setter
    public String getPublicDefaultBucket() { return publicDefaultBucket; }
    public void setPublicDefaultBucket(String publicDefaultBucket) { this.publicDefaultBucket = publicDefaultBucket; }

    public String getPublicImageBucket() { return publicImageBucket; }
    public void setPublicImageBucket(String publicImageBucket) { this.publicImageBucket = publicImageBucket; }

    public String getPublicVideoBucket() { return publicVideoBucket; }
    public void setPublicVideoBucket(String publicVideoBucket) { this.publicVideoBucket = publicVideoBucket; }

    public String getPublicDocumentBucket() { return publicDocumentBucket; }
    public void setPublicDocumentBucket(String publicDocumentBucket) { this.publicDocumentBucket = publicDocumentBucket; }

    public String getPublicAudioBucket() { return publicAudioBucket; }
    public void setPublicAudioBucket(String publicAudioBucket) { this.publicAudioBucket = publicAudioBucket; }

    public String getPublicOtherBucket() { return publicOtherBucket; }
    public void setPublicOtherBucket(String publicOtherBucket) { this.publicOtherBucket = publicOtherBucket; }

    // 私有存储桶 Getter/Setter
    public String getPrivateDefaultBucket() { return privateDefaultBucket; }
    public void setPrivateDefaultBucket(String privateDefaultBucket) { this.privateDefaultBucket = privateDefaultBucket; }

    public String getPrivateImageBucket() { return privateImageBucket; }
    public void setPrivateImageBucket(String privateImageBucket) { this.privateImageBucket = privateImageBucket; }

    public String getPrivateVideoBucket() { return privateVideoBucket; }
    public void setPrivateVideoBucket(String privateVideoBucket) { this.privateVideoBucket = privateVideoBucket; }

    public String getPrivateDocumentBucket() { return privateDocumentBucket; }
    public void setPrivateDocumentBucket(String privateDocumentBucket) { this.privateDocumentBucket = privateDocumentBucket; }

    public String getPrivateAudioBucket() { return privateAudioBucket; }
    public void setPrivateAudioBucket(String privateAudioBucket) { this.privateAudioBucket = privateAudioBucket; }

    public String getPrivateOtherBucket() { return privateOtherBucket; }
    public void setPrivateOtherBucket(String privateOtherBucket) { this.privateOtherBucket = privateOtherBucket; }



    public int getUrlExpireSeconds() { return urlExpireSeconds; }
    public void setUrlExpireSeconds(int urlExpireSeconds) { this.urlExpireSeconds = urlExpireSeconds; }

    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }

    public long getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(long connectTimeout) { this.connectTimeout = connectTimeout; }

    public long getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(long writeTimeout) { this.writeTimeout = writeTimeout; }

    public long getReadTimeout() { return readTimeout; }
    public void setReadTimeout(long readTimeout) { this.readTimeout = readTimeout; }
}