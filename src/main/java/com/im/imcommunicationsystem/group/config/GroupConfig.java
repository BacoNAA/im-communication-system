package com.im.imcommunicationsystem.group.config;

import com.im.imcommunicationsystem.group.utils.GroupUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 群组模块基础配置
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class GroupConfig {

    /**
     * 最大群组数量限制
     */
    @Value("${im.group.max-groups-per-user:100}")
    private int maxGroupsPerUser;

    /**
     * 最大群成员数量
     */
    @Value("${im.group.max-members:500}")
    private int maxMembers;

    /**
     * 邀请码有效期（分钟）
     */
    @Value("${im.group.invite-code-expiry-minutes:1440}")
    private int inviteCodeExpiryMinutes;

    /**
     * 群名称最大长度
     */
    @Value("${im.group.max-name-length:100}")
    private int maxNameLength;

    /**
     * 群介绍最大长度
     */
    @Value("${im.group.max-description-length:500}")
    private int maxDescriptionLength;

    /**
     * 群公告标题最大长度
     */
    @Value("${im.group.max-announcement-title-length:100}")
    private int maxAnnouncementTitleLength;

    /**
     * 群公告内容最大长度
     */
    @Value("${im.group.max-announcement-content-length:5000}")
    private int maxAnnouncementContentLength;

    /**
     * 邀请链接基础URL
     */
    @Value("${im.group.invite-base-url:http://localhost:3001/group}")
    private String inviteBaseUrl;

    /**
     * 获取最大群组数量限制
     * 
     * @return 最大群组数量限制
     */
    public int getMaxGroupsPerUser() {
        return maxGroupsPerUser;
    }

    /**
     * 获取最大群成员数量
     * 
     * @return 最大群成员数量
     */
    public int getMaxMembers() {
        return maxMembers > 0 ? maxMembers : GroupUtils.DEFAULT_MAX_MEMBERS;
    }

    /**
     * 获取邀请码有效期（分钟）
     * 
     * @return 邀请码有效期
     */
    public int getInviteCodeExpiryMinutes() {
        return inviteCodeExpiryMinutes;
    }

    /**
     * 获取群名称最大长度
     * 
     * @return 群名称最大长度
     */
    public int getMaxNameLength() {
        return maxNameLength > 0 ? maxNameLength : GroupUtils.MAX_GROUP_NAME_LENGTH;
    }

    /**
     * 获取群介绍最大长度
     * 
     * @return 群介绍最大长度
     */
    public int getMaxDescriptionLength() {
        return maxDescriptionLength > 0 ? maxDescriptionLength : GroupUtils.MAX_GROUP_DESCRIPTION_LENGTH;
    }

    /**
     * 获取群公告标题最大长度
     * 
     * @return 群公告标题最大长度
     */
    public int getMaxAnnouncementTitleLength() {
        return maxAnnouncementTitleLength;
    }

    /**
     * 获取群公告内容最大长度
     * 
     * @return 群公告内容最大长度
     */
    public int getMaxAnnouncementContentLength() {
        return maxAnnouncementContentLength;
    }

    /**
     * 获取邀请链接基础URL
     * 
     * @return 邀请链接基础URL
     */
    public String getInviteBaseUrl() {
        return inviteBaseUrl;
    }
} 