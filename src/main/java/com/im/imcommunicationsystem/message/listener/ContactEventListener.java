package com.im.imcommunicationsystem.message.listener;

import com.im.imcommunicationsystem.message.entity.Conversation;
import com.im.imcommunicationsystem.message.entity.ConversationMember;
import com.im.imcommunicationsystem.message.entity.ConversationMemberId;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.repository.ConversationMemberRepository;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.message.service.ConversationService;
import com.im.imcommunicationsystem.relationship.event.ContactBlockEvent;
import com.im.imcommunicationsystem.relationship.event.ContactUnblockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 联系人事件监听器
 * 用于处理联系人拉黑和解除拉黑事件
 * 解耦ContactService和ConversationService之间的依赖
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContactEventListener {

    private final ConversationService conversationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository; // 添加MessageRepository

    /**
     * 处理联系人拉黑事件
     * 设置last_acceptable_message_id为拉黑时的最后一条消息
     */
    @EventListener
    @Transactional
    public void handleContactBlockEvent(ContactBlockEvent event) {
        Long userId = event.getUserId();
        Long friendId = event.getFriendId();
        
        log.info("收到拉黑联系人事件: userId={}, friendId={}", userId, friendId);
        
        try {
            // 获取私聊会话
            Conversation conversation = conversationService.getOrCreatePrivateConversation(userId, friendId);
            if (conversation == null) {
                log.warn("设置last_acceptable_message_id失败: 会话不存在, userId={}, friendId={}", userId, friendId);
                return;
            }

            // 直接从MessageRepository获取最后一条消息
            Long lastMessageId = null;
            Optional<Message> lastMessageOpt = messageRepository.findFirstByConversationIdOrderByIdDesc(conversation.getId());
            
            if (lastMessageOpt.isPresent()) {
                lastMessageId = lastMessageOpt.get().getId();
                log.info("从MessageRepository获取到最后一条消息ID: conversationId={}, messageId={}", 
                        conversation.getId(), lastMessageId);
            } else {
                log.warn("会话中没有消息，无法设置last_acceptable_message_id: conversationId={}", conversation.getId());
                // 虽然没有消息，我们仍然需要标记会话成员记录为已拉黑状态
                // 这里我们可以设置lastMessageId为0或者保持null
            }
            
            // 设置会话成员的last_acceptable_message_id
            Optional<ConversationMember> memberOpt = conversationMemberRepository.findById(
                    new ConversationMemberId(conversation.getId(), userId));
            
            if (memberOpt.isPresent()) {
                ConversationMember member = memberOpt.get();
                member.setLastAcceptableMessageId(lastMessageId); // 可能为null
                conversationMemberRepository.save(member);
                
                log.info("设置last_acceptable_message_id成功: conversationId={}, userId={}, messageId={}", 
                        conversation.getId(), userId, lastMessageId);
            } else {
                log.warn("设置last_acceptable_message_id失败: 会话成员不存在, conversationId={}, userId={}", 
                        conversation.getId(), userId);
            }
        } catch (Exception e) {
            log.error("处理拉黑事件异常: userId={}, friendId={}, error={}", 
                    userId, friendId, e.getMessage(), e);
        }
    }

    /**
     * 处理联系人解除拉黑事件
     * 将last_acceptable_message_id设置为NULL
     */
    @EventListener
    @Transactional
    public void handleContactUnblockEvent(ContactUnblockEvent event) {
        Long userId = event.getUserId();
        Long friendId = event.getFriendId();
        
        log.info("收到解除拉黑联系人事件: userId={}, friendId={}", userId, friendId);
        
        try {
            // 获取私聊会话
            Conversation conversation = conversationService.getOrCreatePrivateConversation(userId, friendId);
            if (conversation == null) {
                log.warn("设置last_acceptable_message_id失败: 会话不存在, userId={}, friendId={}", userId, friendId);
                return;
            }
            
            // 设置会话成员的last_acceptable_message_id为null
            Optional<ConversationMember> memberOpt = conversationMemberRepository.findById(
                    new ConversationMemberId(conversation.getId(), userId));
            
            if (memberOpt.isPresent()) {
                ConversationMember member = memberOpt.get();
                member.setLastAcceptableMessageId(null);
                conversationMemberRepository.save(member);
                
                log.info("清除last_acceptable_message_id成功: conversationId={}, userId={}", 
                        conversation.getId(), userId);
            } else {
                log.warn("清除last_acceptable_message_id失败: 会话成员不存在, conversationId={}, userId={}", 
                        conversation.getId(), userId);
            }
        } catch (Exception e) {
            log.error("处理解除拉黑事件异常: userId={}, friendId={}, error={}", 
                    userId, friendId, e.getMessage(), e);
        }
    }
} 