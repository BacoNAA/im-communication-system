package com.im.imcommunicationsystem.message.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 更新会话设置请求类
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationSettingsRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 会话名称
     */
    private String name;

    /**
     * 会话描述
     */
    private String description;

    /**
     * 会话头像URL
     */
    private String avatarUrl;

    /**
     * 会话设置（JSON格式）
     */
    private String settings;

    /**
     * 会话元数据
     */
    private Map<String, Object> metadata;

    /**
     * 是否允许所有成员邀请新成员
     */
    private Boolean allowMemberInvite;

    /**
     * 是否允许所有成员修改群信息
     */
    private Boolean allowMemberModify;

    /**
     * 是否开启消息审核
     */
    private Boolean messageModeration;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 检查是否有任何设置需要更新
     * 
     * @return 是否有更新
     */
    public boolean hasUpdates() {
        return name != null || description != null || avatarUrl != null || 
               settings != null || metadata != null || allowMemberInvite != null ||
               allowMemberModify != null || messageModeration != null || announcement != null;
    }
}