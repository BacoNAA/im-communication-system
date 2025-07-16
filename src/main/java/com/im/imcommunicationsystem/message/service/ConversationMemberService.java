package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.dto.response.ConversationMemberDTO;
import com.im.imcommunicationsystem.message.entity.ConversationMember;


import java.util.List;

/**
 * 会话参与者服务接口
 * 定义会话成员管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface ConversationMemberService {

    /**
     * 添加会话成员
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param inviterId 邀请者ID
     * @return 成员信息
     */
    ConversationMemberDTO addParticipant(Long conversationId, Long userId, Long inviterId);

    /**
     * 移除会话参与者
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param operatorId 操作者ID
     */
    void removeParticipant(Long conversationId, Long userId, Long operatorId);



    /**
     * 获取会话参与者列表
     * 
     * @param conversationId 会话ID
     * @return 参与者列表
     */
    List<ConversationMemberDTO> getParticipants(Long conversationId);

    /**
     * 获取用户在会话中的信息
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 参与者信息
     */
    ConversationMemberDTO getParticipant(Long conversationId, Long userId);

    /**
     * 检查用户是否在会话中
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否在会话中
     */
    boolean isParticipant(Long conversationId, Long userId);

    /**
     * 检查用户是否有管理权限
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否有管理权限
     */
    boolean hasAdminPermission(Long conversationId, Long userId);

    /**
     * 设置免打扰状态
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isDnd 是否开启免打扰
     */
    void setDndStatus(Long conversationId, Long userId, boolean isDnd);

    /**
     * 设置会话置顶
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param pinned 是否置顶
     */
    void setPinned(Long conversationId, Long userId, boolean pinned);

    /**
     * 设置会话归档
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param archived 是否归档
     */
    void setArchived(Long conversationId, Long userId, boolean archived);

    /**
     * 更新最后已读消息ID
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param lastReadMessageId 最后已读消息ID
     */
    void updateLastReadMessageId(Long conversationId, Long userId, Long lastReadMessageId);

    /**
     * 保存草稿
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param draft 草稿内容
     */
    void saveDraft(Long conversationId, Long userId, String draft);

    /**
     * 清除草稿
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void clearDraft(Long conversationId, Long userId);

    /**
     * 用户离开会话
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void leaveConversation(Long conversationId, Long userId);

    /**
     * 获取会话参与者数量
     * 
     * @param conversationId 会话ID
     * @return 参与者数量
     */
    int getParticipantCount(Long conversationId);

    /**
     * 批量添加成员
     * 
     * @param conversationId 会话ID
     * @param userIds 用户ID列表
     * @param inviterId 邀请者ID
     * @return 成员信息列表
     */
    List<ConversationMemberDTO> addParticipants(Long conversationId, List<Long> userIds, Long inviterId);
}