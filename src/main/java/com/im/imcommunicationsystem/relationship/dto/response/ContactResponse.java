package com.im.imcommunicationsystem.relationship.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人响应DTO
 */
@Data
public class ContactResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 好友用户名
     */
    private String friendUsername;

    /**
     * 好友昵称
     */
    private String friendNickname;

    /**
     * 好友头像URL
     */
    private String friendAvatarUrl;

    /**
     * 好友备注
     */
    private String alias;

    /**
     * 是否屏蔽
     */
    private Boolean isBlocked;

    /**
     * 好友状态
     */
    private String friendStatus;

    /**
     * 最后在线时间
     */
    private LocalDateTime lastOnlineTime;

    /**
     * 添加好友时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否在线
     */
    private Boolean isOnline;

    /**
     * 共同好友数量
     */
    private Integer mutualFriendsCount;
}