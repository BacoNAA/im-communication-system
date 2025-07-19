package com.im.imcommunicationsystem.message.service;

import com.im.imcommunicationsystem.message.dto.request.*;
import com.im.imcommunicationsystem.message.dto.response.ConversationResponse;
import com.im.imcommunicationsystem.message.dto.response.ConversationDTO;
import com.im.imcommunicationsystem.message.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 会话服务接口
 * 定义会话管理相关的业务逻辑
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
public interface ConversationService {

    /**
     * 获取会话列表
     * 
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 会话列表
     */
    Page<ConversationResponse> getConversations(Pageable pageable, Long userId);

    /**
     * 获取已归档会话列表
     * 
     * @param pageable 分页参数
     * @param userId 用户ID
     * @return 已归档的会话列表
     */
    Page<ConversationResponse> getArchivedConversations(Pageable pageable, Long userId);

    /**
     * 创建会话
     * 
     * @param request 创建会话请求
     * @param userId 用户ID
     * @return 会话响应
     */
    ConversationResponse createConversation(CreateConversationRequest request, Long userId);

    /**
     * 更新会话设置
     * 
     * @param conversationId 会话ID
     * @param request 更新会话设置请求
     * @param userId 用户ID
     * @return 会话响应
     */
    ConversationResponse updateConversationSettings(Long conversationId, UpdateConversationSettingsRequest request, Long userId);

    /**
     * 置顶会话
     * 
     * @param conversationId 会话ID
     * @param request 置顶会话请求
     * @param userId 用户ID
     */
    void pinConversation(Long conversationId, PinConversationRequest request, Long userId);

    /**
     * 归档会话
     * 
     * @param conversationId 会话ID
     * @param request 归档会话请求
     * @param userId 用户ID
     */
    void archiveConversation(Long conversationId, ArchiveConversationRequest request, Long userId);

    /**
     * 保存草稿
     * 
     * @param conversationId 会话ID
     * @param request 保存草稿请求
     * @param userId 用户ID
     */
    void saveDraft(Long conversationId, SaveDraftRequest request, Long userId);

    /**
     * 根据ID获取会话
     * 
     * @param conversationId 会话ID
     * @return 会话实体
     */
    Conversation getConversationById(Long conversationId);

    /**
     * 获取用户参与的会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> getConversationsByUserId(Long userId);

    /**
     * 检查用户是否参与会话
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否参与
     */
    boolean isUserInConversation(Long conversationId, Long userId);

    /**
     * 删除会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    void deleteConversation(Long conversationId, Long userId);

    /**
     * 构建会话DTO
     * @param conversation 会话实体
     * @param userId 用户ID
     * @return 会话DTO
     */
    ConversationDTO buildConversationDTO(Conversation conversation, Long userId);

    /**
     * 获取或创建私聊会话
     * 
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 会话实体
     */
    Conversation getOrCreatePrivateConversation(Long userId1, Long userId2);

    /**
     * 更新会话最后活跃时间
     * 
     * @param conversationId 会话ID
     */
    void updateLastActiveTime(Long conversationId);

    /**
     * 获取会话的所有成员ID
     * 
     * @param conversationId 会话ID
     * @return 成员ID列表
     */
    List<Long> getConversationMemberIds(Long conversationId);

    /**
     * 设置会话免打扰状态
     * 
     * @param conversationId 会话ID
     * @param request 免打扰请求
     * @param userId 用户ID
     */
    void muteConversation(Long conversationId, MuteConversationRequest request, Long userId);
}