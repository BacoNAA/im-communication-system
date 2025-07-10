package com.im.imcommunicationsystem.user.service.impl;

import com.im.imcommunicationsystem.user.config.MinioConfig;
import com.im.imcommunicationsystem.user.service.MinioLifecycleService;
import io.minio.MinioClient;
import io.minio.GetBucketLifecycleArgs;
import io.minio.SetBucketLifecycleArgs;
import io.minio.DeleteBucketLifecycleArgs;
import io.minio.messages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * MinIO生命周期规则管理服务实现
 * 用于管理MinIO桶的生命周期规则，实现自动清理过期文件
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MinioLifecycleServiceImpl implements MinioLifecycleService {
    
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    
    @Value("${app.file.temporary-cleanup.default-expiration-days:7}")
    private int defaultExpirationDays;
    
    /**
     * 应用启动后初始化公共桶的生命周期规则
     */
    @PostConstruct
    public void initializePublicBucketLifecycleRules() {
        log.info("开始初始化公共桶生命周期规则，默认过期天数: {}", defaultExpirationDays);
        
        try {
            int successCount = setTemporaryFileLifecycleRuleForAllPublicBuckets(defaultExpirationDays);
            log.info("公共桶生命周期规则初始化完成，成功设置: {} 个桶", successCount);
        } catch (Exception e) {
            log.error("初始化公共桶生命周期规则失败", e);
        }
    }
    
    @Override
    public boolean setTemporaryFileLifecycleRule(String bucketName, int expirationDays) {
        try {
            log.info("为桶 {} 设置临时文件生命周期规则，过期天数: {}", bucketName, expirationDays);
            
            // 创建生命周期规则
            LifecycleRule rule = new LifecycleRule(
                Status.ENABLED,
                null, // AbortIncompleteMultipartUpload
                new Expiration((ZonedDateTime) null, expirationDays, null),
                createRuleFilter(), // 使用标签过滤器
                "temporary-files-cleanup", // 规则ID
                null, // NoncurrentVersionExpiration
                null, // NoncurrentVersionTransition
                null  // Transition
            );
            
            // 创建生命周期配置
            List<LifecycleRule> rules = new LinkedList<>();
            rules.add(rule);
            LifecycleConfiguration config = new LifecycleConfiguration(rules);
            
            // 应用生命周期规则到桶
            minioClient.setBucketLifecycle(
                SetBucketLifecycleArgs.builder()
                    .bucket(bucketName)
                    .config(config)
                    .build()
            );
            
            log.info("成功为桶 {} 设置临时文件生命周期规则", bucketName);
            return true;
            
        } catch (Exception e) {
            log.error("为桶 {} 设置生命周期规则失败", bucketName, e);
            return false;
        }
    }
    
    @Override
    public int setTemporaryFileLifecycleRuleForAllPublicBuckets(int expirationDays) {
        String[] publicBuckets = {
            minioConfig.getPublicDefaultBucket(),
            minioConfig.getPublicImageBucket(),
            minioConfig.getPublicVideoBucket(),
            minioConfig.getPublicDocumentBucket()
        };
        
        int successCount = 0;
        
        for (String bucketName : publicBuckets) {
            if (setTemporaryFileLifecycleRule(bucketName, expirationDays)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    @Override
    public boolean removeLifecycleRule(String bucketName) {
        try {
            log.info("删除桶 {} 的生命周期规则", bucketName);
            
            minioClient.deleteBucketLifecycle(
                DeleteBucketLifecycleArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            log.info("成功删除桶 {} 的生命周期规则", bucketName);
            return true;
            
        } catch (Exception e) {
            log.error("删除桶 {} 的生命周期规则失败", bucketName, e);
            return false;
        }
    }
    
    @Override
    public String getLifecycleRules(String bucketName) {
        try {
            LifecycleConfiguration config = minioClient.getBucketLifecycle(
                GetBucketLifecycleArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            if (config != null && config.rules() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("桶 ").append(bucketName).append(" 的生命周期规则:\n");
                
                for (LifecycleRule rule : config.rules()) {
                    sb.append("- 规则ID: ").append(rule.id()).append("\n");
                    sb.append("  状态: ").append(rule.status()).append("\n");
                    
                    if (rule.expiration() != null) {
                        sb.append("  过期天数: ").append(rule.expiration().days()).append("\n");
                    }
                    
                    if (rule.filter() != null && rule.filter().tag() != null) {
                        sb.append("  标签过滤: ").append(rule.filter().tag().key())
                          .append("=").append(rule.filter().tag().value()).append("\n");
                    }
                    
                    sb.append("\n");
                }
                
                return sb.toString();
            } else {
                return "桶 " + bucketName + " 没有设置生命周期规则";
            }
            
        } catch (Exception e) {
            log.error("获取桶 {} 的生命周期规则失败", bucketName, e);
            return "获取生命周期规则失败: " + e.getMessage();
        }
    }
    
    @Override
    public boolean hasLifecycleRule(String bucketName) {
        try {
            LifecycleConfiguration config = minioClient.getBucketLifecycle(
                GetBucketLifecycleArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            return config != null && config.rules() != null && !config.rules().isEmpty();
            
        } catch (Exception e) {
            // 如果桶没有生命周期规则，MinIO会抛出异常
            log.debug("桶 {} 没有生命周期规则或检查失败: {}", bucketName, e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建用于过滤临时文件的规则过滤器
     * 使用标签过滤器来匹配带有"temporary"标签的文件
     */
    private RuleFilter createRuleFilter() {
        // 创建标签过滤器，匹配file_tag=TEMPORARY的文件
        Tag temporaryTag = new Tag("file_tag", "TEMPORARY");
        return new RuleFilter(new AndOperator(null, Map.of(temporaryTag.key(), temporaryTag.value())));
    }
}