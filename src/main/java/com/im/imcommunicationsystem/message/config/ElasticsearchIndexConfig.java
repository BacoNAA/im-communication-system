package com.im.imcommunicationsystem.message.config;

import com.im.imcommunicationsystem.common.config.ElasticsearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.document.Document;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Elasticsearch索引配置类
 * 负责创建和管理Elasticsearch索引
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile({"!test"})
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchIndexConfig {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 应用启动时初始化索引
     */
    @PostConstruct
    public void initIndices() {
        try {
            log.info("开始初始化Elasticsearch索引...");
            createMessageIndex();
            log.info("Elasticsearch索引初始化完成");
        } catch (Exception e) {
            log.error("Elasticsearch索引初始化失败", e);
        }
    }

    /**
     * 创建消息索引
     */
    private void createMessageIndex() {
        String indexName = ElasticsearchConfig.IndexSettings.MESSAGE_INDEX;
        log.info("创建或更新索引: {}", indexName);

        IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));

        // 检查索引是否存在
        if (indexOps.exists()) {
            log.info("索引 {} 已存在，跳过创建", indexName);
            return;
        }

        // 创建索引设置
        Document settings = Document.create();
        settings.put("number_of_shards", ElasticsearchConfig.IndexSettings.NUMBER_OF_SHARDS);
        settings.put("number_of_replicas", ElasticsearchConfig.IndexSettings.NUMBER_OF_REPLICAS);
        settings.put("refresh_interval", ElasticsearchConfig.IndexSettings.REFRESH_INTERVAL);
        settings.put("max_result_window", ElasticsearchConfig.IndexSettings.MAX_RESULT_WINDOW);

        // 创建索引
        boolean result = indexOps.create(settings);
        if (result) {
            log.info("成功创建索引: {}", indexName);
        } else {
            log.warn("创建索引失败: {}", indexName);
        }
    }
} 