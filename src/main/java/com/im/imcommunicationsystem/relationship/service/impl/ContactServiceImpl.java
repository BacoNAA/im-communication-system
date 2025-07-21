package com.im.imcommunicationsystem.relationship.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactId;
import com.im.imcommunicationsystem.relationship.event.ContactBlockEvent;
import com.im.imcommunicationsystem.relationship.event.ContactUnblockEvent;
import com.im.imcommunicationsystem.relationship.repository.ContactRepository;
import com.im.imcommunicationsystem.relationship.service.ContactService;
import com.im.imcommunicationsystem.relationship.service.ContactTagAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 联系人服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactTagAssignmentService contactTagAssignmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> getContactList(Long userId, boolean includeBlocked) {
        log.info("获取用户联系人列表: userId={}, includeBlocked={}", userId, includeBlocked);
        
        try {
            List<Contact> contacts;
            if (includeBlocked) {
                contacts = contactRepository.findByUserId(userId);
            } else {
                contacts = contactRepository.findByUserIdAndIsBlockedFalse(userId);
            }
            
            if (contacts.isEmpty()) {
                log.info("用户没有联系人: userId={}", userId);
                return new ArrayList<>();
            }
            
            // 批量获取所有联系人的标签信息
            List<Long> friendIds = contacts.stream()
                    .map(Contact::getFriendId)
                    .collect(Collectors.toList());
            
            Map<Long, List<ContactTagResponse>> contactTagsMap = 
                    contactTagAssignmentService.getBatchContactTags(userId, friendIds);
            
            List<ContactResponse> contactResponses = new ArrayList<>();
            for (Contact contact : contacts) {
                log.info("处理联系人关系: userId={}, friendId={}, alias={}", 
                        contact.getUserId(), contact.getFriendId(), contact.getAlias());
                
                Optional<User> friendUser = userRepository.findById(contact.getFriendId());
                if (friendUser.isPresent()) {
                    User friend = friendUser.get();
                    log.info("找到好友用户: friendId={}, email={}, nickname={}", 
                            friend.getId(), friend.getEmail(), friend.getNickname());
                    
                    // 获取该联系人的标签信息
                    List<ContactTagResponse> tags = contactTagsMap.getOrDefault(contact.getFriendId(), new ArrayList<>());
                    
                    // 记录原始isBlocked值
                    log.info("联系人拉黑状态详情: userId={}, friendId={}, isBlocked={}", 
                            userId, friend.getId(), contact.getIsBlocked());
                    
                    ContactResponse response = ContactResponse.builder()
                             .userId(userId)
                             .friendId(friend.getId())
                             .friendUsername(friend.getEmail())
                             .nickname(friend.getNickname())
                             .avatarUrl(friend.getAvatarUrl())
                             .alias(contact.getAlias())
                             .isBlocked(contact.getIsBlocked())
                             .addedAt(contact.getCreatedAt())
                             .lastContactTime(contact.getCreatedAt())
                             .tags(tags)
                             .tagCount(tags.size())
                             .build();
                    
                    log.info("构建ContactResponse: userId={}, friendId={}, nickname={}, alias={}, isBlocked={}, tagCount={}", 
                            response.getUserId(), response.getFriendId(), response.getNickname(), 
                            response.getAlias(), response.getIsBlocked(), response.getTagCount());
                    
                    contactResponses.add(response);
                } else {
                    log.warn("未找到好友用户: friendId={}", contact.getFriendId());
                }
            }
            
            log.info("成功获取联系人列表: userId={}, 联系人数量={}", userId, contactResponses.size());
            return contactResponses;
            
        } catch (Exception e) {
            log.error("获取联系人列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContactResponse> getContactDetail(Long userId, Long friendId) {
        log.info("获取联系人详情: userId={}, friendId={}", userId, friendId);
        
        try {
            // 查找联系人关系
            Optional<Contact> contactOpt = contactRepository.findByUserIdAndFriendId(userId, friendId);
            if (contactOpt.isEmpty()) {
                log.warn("联系人关系不存在: userId={}, friendId={}", userId, friendId);
                return Optional.empty();
            }
            
            Contact contact = contactOpt.get();
            
            // 查找好友用户信息
            Optional<User> friendUserOpt = userRepository.findById(friendId);
            if (friendUserOpt.isEmpty()) {
                log.warn("好友用户不存在: friendId={}", friendId);
                return Optional.empty();
            }
            
            User friend = friendUserOpt.get();
            
            // 获取联系人的标签信息
            List<ContactTagResponse> tags = contactTagAssignmentService.getContactTags(userId, friendId);
            
            ContactResponse response = ContactResponse.builder()
                    .userId(userId)
                    .friendId(friend.getId())
                    .friendUsername(friend.getEmail())
                    .nickname(friend.getNickname())
                    .avatarUrl(friend.getAvatarUrl())
                    .alias(contact.getAlias())
                    .isBlocked(contact.getIsBlocked())
                    .addedAt(contact.getCreatedAt())
                    .lastContactTime(contact.getCreatedAt())
                    .tags(tags)
                    .tagCount(tags.size())
                    .build();
            
            log.info("成功获取联系人详情: userId={}, friendId={}, tagCount={}", 
                    userId, friendId, tags.size());
            
            return Optional.of(response);
            
        } catch (Exception e) {
            log.error("获取联系人详情失败: userId={}, friendId={}, error={}", 
                    userId, friendId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public boolean setContactAlias(Long userId, Long friendId, String alias) {
        log.info("设置好友备注: userId={}, friendId={}, alias={}", userId, friendId, alias);
        
        try {
            // 验证参数
            if (userId == null || friendId == null) {
                log.warn("设置好友备注失败: 用户ID或好友ID为空");
                return false;
            }
            
            if (userId.equals(friendId)) {
                log.warn("设置好友备注失败: 不能为自己设置备注");
                return false;
            }
            
            // 验证备注长度（根据配置和数据库字段限制）
            if (alias != null && alias.length() > 50) {
                log.warn("设置好友备注失败: 备注长度超过限制, alias={}", alias);
                return false;
            }
            
            // 查找联系人关系
            Optional<Contact> contactOpt = contactRepository.findByUserIdAndFriendId(userId, friendId);
            if (contactOpt.isEmpty()) {
                log.warn("设置好友备注失败: 联系人关系不存在, userId={}, friendId={}", userId, friendId);
                return false;
            }
            
            // 更新备注
            Contact contact = contactOpt.get();
            contact.setAlias(alias);
            contactRepository.save(contact);
            
            log.info("成功设置好友备注: userId={}, friendId={}, alias={}", userId, friendId, alias);
            return true;
            
        } catch (Exception e) {
            log.error("设置好友备注异常: userId={}, friendId={}, alias={}, error={}", 
                     userId, friendId, alias, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean blockContact(Long userId, Long friendId) {
        log.info("拉黑联系人: userId={}, friendId={}", userId, friendId);
        
        try {
            // 验证参数
            if (userId == null || friendId == null) {
                log.warn("拉黑联系人失败: 用户ID或好友ID为空");
                return false;
            }
            
            if (userId.equals(friendId)) {
                log.warn("拉黑联系人失败: 不能拉黑自己");
                return false;
            }
            
            // 查找联系人关系
            Optional<Contact> contactOpt = contactRepository.findByUserIdAndFriendId(userId, friendId);
            if (contactOpt.isEmpty()) {
                log.warn("拉黑联系人失败: 联系人关系不存在, userId={}, friendId={}", userId, friendId);
                return false;
            }
            
            Contact contact = contactOpt.get();
            
            // 如果联系人已被拉黑，则直接返回成功
            if (Boolean.TRUE.equals(contact.getIsBlocked())) {
                log.info("联系人已被拉黑，无需重复操作: userId={}, friendId={}", userId, friendId);
                return true;
            }
            
            // 更新联系人关系为已拉黑
            contact.setIsBlocked(true);
            contactRepository.save(contact);
            
            // 发布拉黑事件
            log.info("发布拉黑联系人事件: userId={}, friendId={}", userId, friendId);
            eventPublisher.publishEvent(new ContactBlockEvent(this, userId, friendId));
            log.info("拉黑联系人事件已发布: userId={}, friendId={}", userId, friendId);
            
            log.info("成功拉黑联系人: userId={}, friendId={}", userId, friendId);
            return true;
            
        } catch (Exception e) {
            log.error("拉黑联系人异常: userId={}, friendId={}, error={}", 
                     userId, friendId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean unblockContact(Long userId, Long friendId) {
        log.info("解除拉黑联系人: userId={}, friendId={}", userId, friendId);
        
        try {
            // 验证参数
            if (userId == null || friendId == null) {
                log.warn("解除拉黑联系人失败: 用户ID或好友ID为空");
                return false;
            }
            
            if (userId.equals(friendId)) {
                log.warn("解除拉黑联系人失败: 不能操作自己");
                return false;
            }
            
            // 查找联系人关系
            Optional<Contact> contactOpt = contactRepository.findByUserIdAndFriendId(userId, friendId);
            if (contactOpt.isEmpty()) {
                log.warn("解除拉黑联系人失败: 联系人关系不存在, userId={}, friendId={}", userId, friendId);
                return false;
            }
            
            Contact contact = contactOpt.get();
            
            // 如果联系人未被拉黑，则直接返回成功
            if (Boolean.FALSE.equals(contact.getIsBlocked())) {
                log.info("联系人未被拉黑，无需解除: userId={}, friendId={}", userId, friendId);
                return true;
            }
            
            // 更新联系人关系为未拉黑
            contact.setIsBlocked(false);
            contactRepository.save(contact);
            
            // 发布解除拉黑事件
            log.info("发布解除拉黑联系人事件: userId={}, friendId={}", userId, friendId);
            eventPublisher.publishEvent(new ContactUnblockEvent(this, userId, friendId));
            log.info("解除拉黑联系人事件已发布: userId={}, friendId={}", userId, friendId);
            
            log.info("成功解除拉黑联系人: userId={}, friendId={}", userId, friendId);
            return true;
            
        } catch (Exception e) {
            log.error("解除拉黑联系人异常: userId={}, friendId={}, error={}", 
                     userId, friendId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deleteContact(Long userId, Long friendId) {
        try {
            // 先清除联系人标签
            contactTagAssignmentService.clearContactTags(userId, friendId);
            log.info("Cleared contact tags for user {} and friend {}", userId, friendId);
            
            // 删除双向好友关系
            // 删除 userId -> friendId 的关系
            contactRepository.deleteById(new ContactId(userId, friendId));
            // 删除 friendId -> userId 的关系
            contactRepository.deleteById(new ContactId(friendId, userId));
            
            log.info("Successfully deleted contact relationship between user {} and friend {}", userId, friendId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete contact relationship between user {} and friend {}: {}", userId, friendId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean addContact(Long userId, Long friendId) {
        // TODO: 实现添加好友关系逻辑
        return false;
    }

    @Override
    public Optional<Contact> checkContactRelationship(Long userId, Long friendId) {
        // TODO: 实现检查联系人关系状态逻辑
        return Optional.empty();
    }

    @Override
    public boolean isFriend(Long userId1, Long userId2) {
        log.info("检查用户是否为好友关系: userId1={}, userId2={}", userId1, userId2);
        
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            log.warn("检查好友关系失败: 无效的用户ID, userId1={}, userId2={}", userId1, userId2);
        return false;
        }
        
        try {
            // 使用仓库中的方法直接检查双向好友关系
            boolean areFriends = contactRepository.existsMutualFriendship(userId1, userId2);
            log.info("用户好友关系检查结果: userId1={}, userId2={}, areFriends={}", userId1, userId2, areFriends);
            
            return areFriends;
        } catch (Exception e) {
            log.error("检查好友关系时发生异常: userId1={}, userId2={}, error={}", userId1, userId2, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isBlocked(Long userId, Long friendId) {
        log.info("检查用户是否被屏蔽: userId={}, friendId={}", userId, friendId);
        
        if (userId == null || friendId == null || userId.equals(friendId)) {
            log.warn("检查屏蔽状态失败: 无效的用户ID, userId={}, friendId={}", userId, friendId);
        return false;
        }
        
        try {
            // 检查是否存在屏蔽关系
            Optional<Contact> contact = contactRepository.findByUserIdAndFriendId(userId, friendId);
            
            boolean isBlocked = contact.isPresent() && Boolean.TRUE.equals(contact.get().getIsBlocked());
            log.info("用户屏蔽状态检查结果: userId={}, friendId={}, isBlocked={}", userId, friendId, isBlocked);
            
            return isBlocked;
        } catch (Exception e) {
            log.error("检查屏蔽状态时发生异常: userId={}, friendId={}, error={}", userId, friendId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public long getFriendCount(Long userId) {
        // TODO: 实现获取好友数量逻辑
        return 0;
    }

    @Override
    public List<ContactResponse> searchContacts(Long userId, String keyword) {
        // TODO: 实现搜索联系人逻辑
        return null;
    }

    @Override
    public List<ContactResponse> getBlockedContacts(Long userId) {
        // TODO: 实现获取被屏蔽的联系人列表逻辑
        return null;
    }

    @Override
    @Transactional
    public int batchDeleteContacts(Long userId, List<Long> friendIds) {
        // TODO: 实现批量删除联系人逻辑
        return 0;
    }

    @Override
    @Transactional
    public int batchBlockContacts(Long userId, List<Long> friendIds) {
        // TODO: 实现批量屏蔽联系人逻辑
        return 0;
    }
}