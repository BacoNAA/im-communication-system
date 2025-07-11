package com.im.imcommunicationsystem.relationship.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联系人搜索响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "联系人搜索响应")
public class ContactSearchResponse {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    /**
     * 用户头像URL
     */
    @Schema(description = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    /**
     * 用户个性签名
     */
    @Schema(description = "用户个性签名", example = "这是一个个性签名")
    private String signature;

    /**
     * 是否已经是好友
     */
    @Schema(description = "是否已经是好友", example = "false")
    private Boolean isFriend;

    /**
     * 是否已发送好友请求
     */
    @Schema(description = "是否已发送好友请求", example = "false")
    private Boolean hasRequestSent;

    /**
     * 是否被屏蔽
     */
    @Schema(description = "是否被屏蔽", example = "false")
    private Boolean isBlocked;

    /**
     * 关系状态描述
     */
    @Schema(description = "关系状态描述", example = "可以添加为好友")
    private String relationshipStatus;

    /**
     * 用户注册时间
     */
    @Schema(description = "用户注册时间")
    private LocalDateTime registeredAt;

    /**
     * 是否允许被搜索
     */
    @Schema(description = "是否允许被搜索", example = "true")
    private Boolean allowSearch;

    /**
     * 用户自定义ID字符串
     */
    @Schema(description = "用户自定义ID字符串", example = "user123")
    private String userIdString;
}