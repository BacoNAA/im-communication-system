package com.im.imcommunicationsystem.message.dto.request;

import com.im.imcommunicationsystem.message.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建会话请求对象
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    /**
     * 会话类型
     */
    @NotNull(message = "会话类型不能为空")
    private ConversationType type;

    /**
     * 会话名称（群聊时必填）
     */
    @Size(max = 100, message = "会话名称不能超过100个字符")
    private String name;

    /**
     * 会话描述（可选）
     */
    @Size(max = 500, message = "会话描述不能超过500个字符")
    private String description;

    /**
     * 会话头像URL（可选）
     */
    @Size(max = 500, message = "头像URL不能超过500个字符")
    private String avatarUrl;

    /**
     * 参与者用户ID列表
     */
    private List<Long> participantIds;

    /**
     * 目标用户ID（私聊时使用）
     */
    private Long targetUserId;

    /**
     * 会话设置（JSON格式，可选）
     */
    private String settings;

    /**
     * 会话元数据（JSON格式，可选）
     */
    private String metadata;

    /**
     * 验证请求参数
     * 
     * @return 验证结果
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        
        switch (type) {
            case PRIVATE:
            case TEMPORARY:
                // 私聊必须指定目标用户ID
                return targetUserId != null;
                
            case GROUP:
            case CHANNEL:
                // 群聊必须有名称和参与者
                return name != null && !name.trim().isEmpty() && 
                       participantIds != null && !participantIds.isEmpty();
                
            case SYSTEM:
                // 系统会话不需要额外验证
                return true;
                
            default:
                return false;
        }
    }

    /**
     * 检查是否为私聊
     * 
     * @return 是否为私聊
     */
    public boolean isPrivate() {
        return type != null && type.isPrivate();
    }

    /**
     * 检查是否为群聊
     * 
     * @return 是否为群聊
     */
    public boolean isGroup() {
        return type != null && type.isGroup();
    }

    /**
     * 检查是否为系统会话
     * 
     * @return 是否为系统会话
     */
    public boolean isSystem() {
        return type != null && type.isSystem();
    }

    /**
     * 获取有效名称
     * 
     * @return 有效名称
     */
    public String getEffectiveName() {
        return name != null ? name.trim() : "";
    }

    /**
     * 获取有效描述
     * 
     * @return 有效描述
     */
    public String getEffectiveDescription() {
        return description != null ? description.trim() : "";
    }

    /**
     * 获取参与者数量
     * 
     * @return 参与者数量
     */
    public int getParticipantCount() {
        if (isPrivate()) {
            return 2; // 私聊固定2人
        }
        
        if (participantIds != null) {
            return participantIds.size() + 1; // 参与者 + 创建者
        }
        
        return 1; // 只有创建者
    }

    /**
     * 检查参与者数量是否有效
     * 
     * @return 是否有效
     */
    public boolean isValidParticipantCount() {
        if (type == null) {
            return false;
        }
        
        return type.isValidParticipantCount(getParticipantCount());
    }
}