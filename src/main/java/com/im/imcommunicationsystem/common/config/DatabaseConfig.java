package com.im.imcommunicationsystem.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库配置类
 * 配置JPA、事务管理和实体扫描
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.im.imcommunicationsystem")
@EntityScan(basePackages = "com.im.imcommunicationsystem")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * 数据库配置说明：
     * 
     * 1. @EnableJpaRepositories: 启用JPA仓库，自动扫描指定包下的Repository接口
     * 2. @EntityScan: 扫描JPA实体类，确保所有实体都被Spring管理
     * 3. @EnableJpaAuditing: 启用JPA审计功能，自动填充创建时间、修改时间等字段
     * 4. @EnableTransactionManagement: 启用事务管理，支持@Transactional注解
     * 
     * 数据库连接配置在application.yml中定义：
     * - 数据源URL、用户名、密码
     * - 连接池配置
     * - JPA/Hibernate配置
     * 
     * 支持的数据库操作：
     * - 自动建表（开发环境）
     * - 数据库迁移（生产环境）
     * - 连接池管理
     * - 事务控制
     * - 审计日志
     */

}