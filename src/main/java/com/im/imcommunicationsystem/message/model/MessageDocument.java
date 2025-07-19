package com.im.imcommunicationsystem.message.model;

import com.im.imcommunicationsystem.common.config.ElasticsearchConfig.AnalyzerSettings;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * 消息文档模型类
 * 用于Elasticsearch索引
 */
@Document(indexName = "im_messages")
@Setting(
        shards = 3, 
        replicas = 1, 
        refreshInterval = "1s",
        settingPath = ""
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long conversationId;

    @Field(type = FieldType.Long)
    private Long senderId;
    
    // 这些字段在索引时会被填充，但不直接来自Message实体
    @Field(type = FieldType.Keyword)
    private String senderName;
    
    @Field(type = FieldType.Keyword)
    private String senderAvatar;

    @Field(
            type = FieldType.Text,
            analyzer = AnalyzerSettings.STANDARD_ANALYZER,
            searchAnalyzer = AnalyzerSettings.STANDARD_ANALYZER
    )
    private String content;

    @Field(type = FieldType.Keyword)
    private MessageType messageType;

    @Field(type = FieldType.Keyword)
    private MessageStatus status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Boolean)
    private Boolean edited;
    
    @Field(type = FieldType.Long)
    private Long mediaFileId;
    
    // 这个字段在索引时会被填充，但不直接来自Message实体
    @Field(type = FieldType.Keyword)
    private String fileName;
    
    @Field(type = FieldType.Boolean)
    private Boolean isRead;

    /**
     * 从消息实体转换为文档
     * @param message 消息实体
     * @return 消息文档
     */
    public static MessageDocument fromEntity(Message message) {
        if (message == null) {
            return null;
        }
        
        return MessageDocument.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .edited(message.getEdited())
                .mediaFileId(message.getMediaFileId())
                .isRead(message.getIsRead())
                .build();
    }
} 