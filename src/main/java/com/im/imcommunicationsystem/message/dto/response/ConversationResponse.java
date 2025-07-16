package com.im.imcommunicationsystem.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话响应类
 * 用于返回会话相关的响应数据
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    /**
     * 会话信息
     */
    private ConversationDTO conversation;

    /**
     * 会话列表
     */
    private List<ConversationDTO> conversations;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 创建单个会话响应
     * 
     * @param conversation 会话信息
     * @return 会话响应
     */
    public static ConversationResponse success(ConversationDTO conversation) {
        return ConversationResponse.builder()
                .conversation(conversation)
                .success(true)
                .build();
    }

    /**
     * 创建会话列表响应
     * 
     * @param conversations 会话列表
     * @return 会话响应
     */
    public static ConversationResponse success(List<ConversationDTO> conversations) {
        return ConversationResponse.builder()
                .conversations(conversations)
                .success(true)
                .build();
    }

    /**
     * 创建成功响应
     * 
     * @param message 响应消息
     * @return 会话响应
     */
    public static ConversationResponse success(String message) {
        return ConversationResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 创建失败响应
     * 
     * @param message 错误消息
     * @return 会话响应
     */
    public static ConversationResponse error(String message) {
        return ConversationResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}