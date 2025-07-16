package com.im.imcommunicationsystem.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息响应类
 * 用于返回消息相关的响应数据
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /**
     * 消息信息
     */
    private MessageDTO message;

    /**
     * 消息列表
     */
    private List<MessageDTO> messages;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String responseMessage;

    /**
     * 总数量（用于分页）
     */
    private Long total;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;

    /**
     * 创建单个消息响应
     * 
     * @param message 消息信息
     * @return 消息响应
     */
    public static MessageResponse success(MessageDTO message) {
        return MessageResponse.builder()
                .message(message)
                .success(true)
                .build();
    }

    /**
     * 创建消息列表响应
     * 
     * @param messages 消息列表
     * @return 消息响应
     */
    public static MessageResponse success(List<MessageDTO> messages) {
        return MessageResponse.builder()
                .messages(messages)
                .success(true)
                .build();
    }

    /**
     * 创建分页消息列表响应
     * 
     * @param messages 消息列表
     * @param total 总数量
     * @param hasMore 是否还有更多
     * @return 消息响应
     */
    public static MessageResponse success(List<MessageDTO> messages, Long total, Boolean hasMore) {
        return MessageResponse.builder()
                .messages(messages)
                .total(total)
                .hasMore(hasMore)
                .success(true)
                .build();
    }

    /**
     * 创建成功响应
     * 
     * @param responseMessage 响应消息
     * @return 消息响应
     */
    public static MessageResponse success(String responseMessage) {
        return MessageResponse.builder()
                .success(true)
                .responseMessage(responseMessage)
                .build();
    }

    /**
     * 创建失败响应
     * 
     * @param responseMessage 错误消息
     * @return 消息响应
     */
    public static MessageResponse error(String responseMessage) {
        return MessageResponse.builder()
                .success(false)
                .responseMessage(responseMessage)
                .build();
    }
}