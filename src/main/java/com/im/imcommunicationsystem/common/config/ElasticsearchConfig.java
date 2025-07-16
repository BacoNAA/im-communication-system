package com.im.imcommunicationsystem.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import jakarta.annotation.PreDestroy;
import java.time.Duration;

/**
 * Elasticsearch配置类
 * 配置Elasticsearch客户端和相关设置
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.im.imcommunicationsystem.message.repository.elasticsearch")
@Profile({"!test"}) // 非测试环境才启用
@Slf4j
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.scheme:http}")
    private String scheme;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${elasticsearch.socket-timeout:30000}")
    private int socketTimeout;

    @Value("${elasticsearch.max-retry-timeout:30000}")
    private int maxRetryTimeout;

    @Value("${elasticsearch.max-connections:100}")
    private int maxConnections;

    @Value("${elasticsearch.max-connections-per-route:10}")
    private int maxConnectionsPerRoute;

    @Override
    public ClientConfiguration clientConfiguration() {
        log.info("初始化Elasticsearch客户端配置，连接地址: {}://{}:{}", scheme, host, port);
        
        ClientConfiguration config;
        
        if ("https".equalsIgnoreCase(scheme)) {
            // HTTPS配置
            if (username != null && !username.isEmpty() && 
                password != null && !password.isEmpty()) {
                config = ClientConfiguration.builder()
                        .connectedTo(host + ":" + port)
                        .usingSsl()
                        .withBasicAuth(username, password)
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withSocketTimeout(Duration.ofMillis(socketTimeout))
                        .build();
                log.info("Elasticsearch客户端已配置HTTPS和基本认证");
            } else {
                config = ClientConfiguration.builder()
                        .connectedTo(host + ":" + port)
                        .usingSsl()
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withSocketTimeout(Duration.ofMillis(socketTimeout))
                        .build();
                log.info("Elasticsearch客户端已配置HTTPS");
            }
        } else {
            // HTTP配置
            if (username != null && !username.isEmpty() && 
                password != null && !password.isEmpty()) {
                config = ClientConfiguration.builder()
                        .connectedTo(host + ":" + port)
                        .withBasicAuth(username, password)
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withSocketTimeout(Duration.ofMillis(socketTimeout))
                        .build();
                log.info("Elasticsearch客户端已配置基本认证");
            } else {
                config = ClientConfiguration.builder()
                        .connectedTo(host + ":" + port)
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withSocketTimeout(Duration.ofMillis(socketTimeout))
                        .build();
                log.info("Elasticsearch客户端已配置基本连接");
            }
        }
        
        log.info("Elasticsearch客户端配置初始化成功");
        return config;
    }

    /**
     * 应用关闭时清理资源
     * 注意：在新的Elasticsearch客户端中，连接管理由Spring自动处理
     */
    @PreDestroy
    public void cleanup() {
        log.info("Elasticsearch配置清理完成");
    }

    /**
     * 获取索引设置
     */
    public static class IndexSettings {
        public static final String MESSAGE_INDEX = "im_messages";
        public static final String USER_INDEX = "im_users";
        public static final String CONVERSATION_INDEX = "im_conversations";
        
        // 分片和副本设置
        public static final int NUMBER_OF_SHARDS = 3;
        public static final int NUMBER_OF_REPLICAS = 1;
        
        // 刷新间隔
        public static final String REFRESH_INTERVAL = "1s";
        
        // 最大结果窗口
        public static final int MAX_RESULT_WINDOW = 10000;
    }

    /**
     * 分析器配置
     */
    public static class AnalyzerSettings {
        // 中文分析器
        public static final String IK_ANALYZER = "ik_max_word";
        public static final String IK_SEARCH_ANALYZER = "ik_smart";
        
        // 标准分析器
        public static final String STANDARD_ANALYZER = "standard";
        
        // 关键词分析器
        public static final String KEYWORD_ANALYZER = "keyword";
    }

    /**
     * 字段映射配置
     */
    public static class FieldMappings {
        // 文本字段配置
        public static final String TEXT_FIELD_TYPE = "text";
        public static final String KEYWORD_FIELD_TYPE = "keyword";
        public static final String DATE_FIELD_TYPE = "date";
        public static final String LONG_FIELD_TYPE = "long";
        public static final String INTEGER_FIELD_TYPE = "integer";
        public static final String BOOLEAN_FIELD_TYPE = "boolean";
        
        // 日期格式
        public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis";
    }
}