package com.im.imcommunicationsystem.user.service;

import io.minio.messages.LifecycleRule;

/**
 * MinIO生命周期规则管理服务接口
 * 用于管理MinIO桶的生命周期规则，实现自动清理过期文件
 */
public interface MinioLifecycleService {
    
    /**
     * 为指定桶设置临时文件生命周期规则
     * 
     * @param bucketName 桶名称
     * @param expirationDays 过期天数
     * @return 是否设置成功
     */
    boolean setTemporaryFileLifecycleRule(String bucketName, int expirationDays);
    
    /**
     * 为所有公共桶设置临时文件生命周期规则
     * 
     * @param expirationDays 过期天数
     * @return 设置成功的桶数量
     */
    int setTemporaryFileLifecycleRuleForAllPublicBuckets(int expirationDays);
    
    /**
     * 删除指定桶的生命周期规则
     * 
     * @param bucketName 桶名称
     * @return 是否删除成功
     */
    boolean removeLifecycleRule(String bucketName);
    
    /**
     * 获取指定桶的生命周期规则
     * 
     * @param bucketName 桶名称
     * @return 生命周期规则列表
     */
    String getLifecycleRules(String bucketName);
    
    /**
     * 检查指定桶是否已设置生命周期规则
     * 
     * @param bucketName 桶名称
     * @return 是否已设置
     */
    boolean hasLifecycleRule(String bucketName);
    
    /**
     * 初始化所有公共桶的生命周期规则
     * 在应用启动时调用，确保所有公共桶都有正确的生命周期规则
     */
    void initializePublicBucketLifecycleRules();
}