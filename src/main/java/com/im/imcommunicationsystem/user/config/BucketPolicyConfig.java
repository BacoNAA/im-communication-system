package com.im.imcommunicationsystem.user.config;

import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 存储桶策略配置类
 * 负责设置公开和私有存储桶的访问策略
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BucketPolicyConfig implements CommandLineRunner {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始配置存储桶访问策略...");
        
        try {
            // 配置公开存储桶策略
            configurePublicBucketPolicies();
            
            // 配置私有存储桶策略
            configurePrivateBucketPolicies();
            
            log.info("存储桶访问策略配置完成");
        } catch (Exception e) {
            log.error("配置存储桶访问策略失败", e);
        }
    }
    
    /**
     * 配置公开存储桶策略
     * 允许匿名读取访问
     */
    private void configurePublicBucketPolicies() {
        String[] publicBuckets = {
            minioConfig.getPublicDefaultBucket(),
            minioConfig.getPublicImageBucket(),
            minioConfig.getPublicVideoBucket(),
            minioConfig.getPublicAudioBucket(),
            minioConfig.getPublicDocumentBucket(),
            minioConfig.getPublicOtherBucket()
        };
        
        for (String bucketName : publicBuckets) {
            try {
                String policy = generatePublicReadPolicy(bucketName);
                minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
                );
                log.info("已设置公开存储桶策略: {}", bucketName);
            } catch (Exception e) {
                log.warn("设置公开存储桶策略失败: {}, 错误: {}", bucketName, e.getMessage());
            }
        }
    }
    
    /**
     * 配置私有存储桶策略
     * 拒绝匿名访问
     */
    private void configurePrivateBucketPolicies() {
        String[] privateBuckets = {
            minioConfig.getPrivateDefaultBucket(),
            minioConfig.getPrivateImageBucket(),
            minioConfig.getPrivateVideoBucket(),
            minioConfig.getPrivateAudioBucket(),
            minioConfig.getPrivateDocumentBucket(),
            minioConfig.getPrivateOtherBucket()
        };
        
        for (String bucketName : privateBuckets) {
            try {
                String policy = generatePrivatePolicy(bucketName);
                minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
                );
                log.info("已设置私有存储桶策略: {}", bucketName);
            } catch (Exception e) {
                log.warn("设置私有存储桶策略失败: {}, 错误: {}", bucketName, e.getMessage());
            }
        }
    }
    
    /**
     * 生成公开读取策略
     * 允许匿名用户读取存储桶中的对象
     */
    private String generatePublicReadPolicy(String bucketName) {
        return String.format(
            "{" +
            "\"Version\": \"2012-10-17\"," +
            "\"Statement\": [" +
                "{" +
                    "\"Effect\": \"Allow\"," +
                    "\"Principal\": {\"AWS\": \"*\"}," +
                    "\"Action\": [" +
                        "\"s3:GetObject\"," +
                        "\"s3:GetObjectVersion\"" +
                    "]," +
                    "\"Resource\": [\"arn:aws:s3:::%s/*\"]" +
                "}," +
                "{" +
                    "\"Effect\": \"Allow\"," +
                    "\"Principal\": {\"AWS\": \"*\"}," +
                    "\"Action\": [" +
                        "\"s3:ListBucket\"," +
                        "\"s3:GetBucketLocation\"" +
                    "]," +
                    "\"Resource\": [\"arn:aws:s3:::%s\"]" +
                "}" +
            "]" +
            "}",
            bucketName, bucketName);
    }
    
    /**
     * 生成私有策略
     * 拒绝匿名访问，只允许认证用户访问
     */
    private String generatePrivatePolicy(String bucketName) {
        return String.format(
            "{" +
            "\"Version\": \"2012-10-17\"," +
            "\"Statement\": [" +
                "{" +
                    "\"Effect\": \"Deny\"," +
                    "\"Principal\": {\"AWS\": \"*\"}," +
                    "\"Action\": [\"s3:*\"]," +
                    "\"Resource\": [" +
                        "\"arn:aws:s3:::%s\"," +
                        "\"arn:aws:s3:::%s/*\"" +
                    "]," +
                    "\"Condition\": {" +
                        "\"StringEquals\": {" +
                            "\"aws:PrincipalType\": \"Anonymous\"" +
                        "}" +
                    "}" +
                "}" +
            "]" +
            "}",
            bucketName, bucketName);
    }
    
    /**
     * 生成受限访问策略
     * 只允许特定用户或角色访问
     */
    private String generateRestrictedPolicy(String bucketName, String[] allowedPrincipals) {
        StringBuilder principals = new StringBuilder();
        for (int i = 0; i < allowedPrincipals.length; i++) {
            if (i > 0) principals.append(", ");
            principals.append("\"").append(allowedPrincipals[i]).append("\"");
        }
        
        return String.format(
            "{" +
            "\"Version\": \"2012-10-17\"," +
            "\"Statement\": [" +
                "{" +
                    "\"Effect\": \"Allow\"," +
                    "\"Principal\": {\"AWS\": [%s]}," +
                    "\"Action\": [" +
                        "\"s3:GetObject\"," +
                        "\"s3:GetObjectVersion\"," +
                        "\"s3:PutObject\"," +
                        "\"s3:DeleteObject\"" +
                    "]," +
                    "\"Resource\": [\"arn:aws:s3:::%s/*\"]" +
                "}," +
                "{" +
                    "\"Effect\": \"Allow\"," +
                    "\"Principal\": {\"AWS\": [%s]}," +
                    "\"Action\": [" +
                        "\"s3:ListBucket\"," +
                        "\"s3:GetBucketLocation\"" +
                    "]," +
                    "\"Resource\": [\"arn:aws:s3:::%s\"]" +
                "}," +
                "{" +
                    "\"Effect\": \"Deny\"," +
                    "\"Principal\": {\"AWS\": \"*\"}," +
                    "\"Action\": [\"s3:*\"]," +
                    "\"Resource\": [" +
                        "\"arn:aws:s3:::%s\"," +
                        "\"arn:aws:s3:::%s/*\"" +
                    "]," +
                    "\"Condition\": {" +
                        "\"StringNotEquals\": {" +
                            "\"aws:PrincipalArn\": [%s]" +
                        "}" +
                    "}" +
                "}" +
            "]" +
            "}",
            principals, bucketName, principals, bucketName, bucketName, bucketName, principals);
    }
    
    /**
     * 重置存储桶策略为默认状态
     */
    public void resetBucketPolicy(String bucketName) {
        try {
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config("")
                    .build()
            );
            log.info("已重置存储桶策略: {}", bucketName);
        } catch (Exception e) {
            log.error("重置存储桶策略失败: {}", bucketName, e);
        }
    }
    
    /**
     * 为特定存储桶设置自定义策略
     */
    public void setCustomBucketPolicy(String bucketName, String policy) {
        try {
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build()
            );
            log.info("已设置自定义存储桶策略: {}", bucketName);
        } catch (Exception e) {
            log.error("设置自定义存储桶策略失败: {}", bucketName, e);
        }
    }
}