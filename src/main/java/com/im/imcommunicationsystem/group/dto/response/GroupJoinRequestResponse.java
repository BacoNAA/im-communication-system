package com.im.imcommunicationsystem.group.dto.response;

import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组加入请求响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequestResponse {
    
    /**
     * 请求ID
     */
    private Long id;
    
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 群组名称
     */
    private String groupName;
    
    /**
     * 群组头像URL
     */
    private String groupAvatarUrl;
    
    /**
     * 群组描述
     */
    private String groupDescription;
    
    /**
     * 群组成员数量
     */
    private Integer groupMemberCount;
    
    /**
     * 申请者ID
     */
    private Long userId;
    
    /**
     * 申请者用户名
     */
    private String username;
    
    /**
     * 申请者昵称
     */
    private String nickname;
    
    /**
     * 申请者头像URL
     */
    private String avatarUrl;
    
    /**
     * 申请消息
     */
    private String message;
    
    /**
     * 申请状态
     */
    private GroupJoinRequestStatus status;
    
    /**
     * 状态描述
     */
    private String statusDescription;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
    
    /**
     * 处理者ID
     */
    private Long handlerId;
    
    /**
     * 处理者用户名
     */
    private String handlerUsername;
    
    /**
     * 处理者昵称
     */
    private String handlerNickname;
    
    /**
     * 是否可以处理（管理员）
     */
    private boolean canHandle;
    
    /**
     * 是否可以取消（申请人）
     */
    private boolean canCancel;
} 