package com.im.imcommunicationsystem.relationship.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagAssignRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagAssignmentResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactTag;
import com.im.imcommunicationsystem.relationship.entity.ContactTagAssignment;
import com.im.imcommunicationsystem.relationship.exception.ContactTagException;
import com.im.imcommunicationsystem.relationship.repository.ContactRepository;
import com.im.imcommunicationsystem.relationship.repository.ContactTagAssignmentRepository;
import com.im.imcommunicationsystem.relationship.repository.ContactTagRepository;
import com.im.imcommunicationsystem.relationship.service.ContactTagAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * 联系人标签分配服务实现类
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ContactTagAssignmentServiceImpl implements ContactTagAssignmentService {

    private final ContactTagAssignmentRepository assignmentRepository;
    private final ContactTagRepository contactTagRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    @Override
    public List<ContactTagAssignmentResponse> assignTagsToContact(ContactTagAssignRequest request) {
        // 验证请求参数
        if (request.getTagIds() == null || request.getTagIds().isEmpty()) {
            throw new ContactTagException("标签ID列表不能为空");
        }

        // 验证所有标签都属于该用户
        List<ContactTag> tags = contactTagRepository.findAllById(request.getTagIds());
        if (tags.size() != request.getTagIds().size()) {
            throw new ContactTagException("部分标签不存在");
        }

        for (ContactTag tag : tags) {
            if (!tag.getUserId().equals(request.getUserId())) {
                throw new ContactTagException("无权限使用标签: " + tag.getName());
            }
        }

        List<ContactTagAssignmentResponse> responses = new ArrayList<>();

        for (Long tagId : request.getTagIds()) {
            // 检查是否已经分配过
            if (!assignmentRepository.existsByUserIdAndFriendIdAndTagId(
                    request.getUserId(), request.getFriendId(), tagId)) {
                
                // 创建新的分配记录
                ContactTagAssignment assignment = ContactTagAssignment.builder()
                        .userId(request.getUserId())
                        .friendId(request.getFriendId())
                        .tagId(tagId)
                        .build();

                ContactTagAssignment saved = assignmentRepository.save(assignment);
                responses.add(convertToResponse(saved, tags));
            }
        }

        return responses;
    }

    @Override
    public int removeTagsFromContact(Long userId, Long friendId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }

        int removedCount = 0;
        for (Long tagId : tagIds) {
            if (validateAssignmentPermission(userId, friendId, tagId)) {
                assignmentRepository.deleteByUserIdAndFriendIdAndTagId(userId, friendId, tagId);
                removedCount++;
            }
        }

        return removedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactTagResponse> getContactTags(Long userId, Long friendId) {
        List<ContactTagAssignment> assignments = assignmentRepository
                .findByUserIdAndFriendId(userId, friendId);

        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = assignments.stream()
                .map(ContactTagAssignment::getTagId)
                .collect(Collectors.toList());

        List<ContactTag> tags = contactTagRepository.findAllById(tagIds);
        
        return tags.stream()
                .map(this::convertTagToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getContactsByTag(Long userId, Long tagId) {
        // 验证标签权限
        ContactTag tag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("标签不存在"));
        
        if (!tag.getUserId().equals(userId)) {
            throw new ContactTagException("无权限访问此标签");
        }

        return assignmentRepository.findByUserIdAndTagId(userId, tagId)
                .stream()
                .map(ContactTagAssignment::getFriendId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<ContactTagResponse>> getBatchContactTags(Long userId, List<Long> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ContactTagAssignment> assignments = assignmentRepository
                .findByUserIdAndFriendIdIn(userId, friendIds);

        // 获取所有相关的标签
        Set<Long> tagIds = assignments.stream()
                .map(ContactTagAssignment::getTagId)
                .collect(Collectors.toSet());

        Map<Long, ContactTag> tagMap = contactTagRepository.findAllById(tagIds)
                .stream()
                .collect(Collectors.toMap(ContactTag::getId, tag -> tag));

        // 按好友ID分组
        Map<Long, List<ContactTagResponse>> result = new HashMap<>();
        
        for (Long friendId : friendIds) {
            result.put(friendId, new ArrayList<>());
        }

        for (ContactTagAssignment assignment : assignments) {
            ContactTag tag = tagMap.get(assignment.getTagId());
            if (tag != null) {
                ContactTagResponse tagResponse = convertTagToResponse(tag);
                result.get(assignment.getFriendId()).add(tagResponse);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public List<ContactTagAssignmentResponse> replaceContactTags(Long userId, Long friendId, List<Long> tagIds) {
        // 如果新标签列表为空，只清除现有标签
        if (tagIds == null || tagIds.isEmpty()) {
            clearContactTags(userId, friendId);
            return new ArrayList<>();
        }

        // 验证所有标签都属于该用户（在清除之前验证，避免数据不一致）
        List<ContactTag> tags = contactTagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new ContactTagException("部分标签不存在");
        }

        for (ContactTag tag : tags) {
            if (!tag.getUserId().equals(userId)) {
                throw new ContactTagException("无权限使用标签: " + tag.getName());
            }
        }

        // 先清除现有标签
        clearContactTags(userId, friendId);

        // 分配新标签
        ContactTagAssignRequest request = new ContactTagAssignRequest();
        request.setUserId(userId);
        request.setFriendId(friendId);
        request.setTagIds(tagIds);

        return assignTagsToContact(request);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTag(Long userId, Long friendId, Long tagId) {
        return assignmentRepository.existsByUserIdAndFriendIdAndTagId(userId, friendId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getContactTagCount(Long userId, Long friendId) {
        return (int) assignmentRepository.countByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public int clearContactTags(Long userId, Long friendId) {
        List<ContactTagAssignment> assignments = assignmentRepository
                .findByUserIdAndFriendId(userId, friendId);
        
        if (!assignments.isEmpty()) {
            assignmentRepository.deleteAll(assignments);
        }
        
        return assignments.size();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTagUsageCount(Long userId, Long tagId) {
        // 验证标签权限
        ContactTag tag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("标签不存在"));
        
        if (!tag.getUserId().equals(userId)) {
            throw new ContactTagException("无权限访问此标签");
        }

        return assignmentRepository.countByTagId(tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateAssignmentPermission(Long userId, Long friendId, Long tagId) {
        // 验证标签是否属于该用户
        ContactTag tag = contactTagRepository.findById(tagId).orElse(null);
        if (tag == null || !tag.getUserId().equals(userId)) {
            return false;
        }

        // 这里可以添加更多的权限验证逻辑，比如验证好友关系等
        // 目前简单验证标签所有权即可
        return true;
    }

    /**
     * 转换分配记录为响应DTO
     */
    private ContactTagAssignmentResponse convertToResponse(ContactTagAssignment assignment, List<ContactTag> tags) {
        ContactTag tag = tags.stream()
                .filter(t -> t.getId().equals(assignment.getTagId()))
                .findFirst()
                .orElse(null);

        ContactTagAssignmentResponse response = new ContactTagAssignmentResponse();
        response.setAssignmentId(assignment.getId());
        response.setUserId(assignment.getUserId());
        response.setFriendId(assignment.getFriendId());
        response.setTagId(assignment.getTagId());
        response.setCreatedAt(assignment.getCreatedAt());
        
        if (tag != null) {
            response.setTagName(tag.getName());
            response.setTagColor(tag.getColor());
        }

        return response;
    }

    /**
     * 转换标签实体为响应DTO
     */
    private ContactTagResponse convertTagToResponse(ContactTag tag) {
        ContactTagResponse response = new ContactTagResponse();
        response.setTagId(tag.getId());
        response.setUserId(tag.getUserId());
        response.setName(tag.getName());
        response.setColor(tag.getColor());
        response.setCreatedAt(tag.getCreatedAt());
        response.setIsDefault(false);
        response.setSortOrder(0);
        
        // 获取使用次数
        long contactCount = assignmentRepository.countByTagId(tag.getId());
        response.setContactCount((int) contactCount);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> getContactDetailsByTag(Long userId, Long tagId) {
        // 验证标签权限
        ContactTag tag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("标签不存在"));
        
        if (!tag.getUserId().equals(userId)) {
            throw new ContactTagException("无权限访问此标签");
        }

        // 获取标签下的所有联系人ID
        List<Long> contactIds = assignmentRepository.findByUserIdAndTagId(userId, tagId)
                .stream()
                .map(ContactTagAssignment::getFriendId)
                .collect(Collectors.toList());

        if (contactIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取联系人详细信息
        List<ContactResponse> contacts = new ArrayList<>();
        for (Long contactId : contactIds) {
            // 查找联系人关系
            Optional<Contact> contactOpt = contactRepository.findByUserIdAndFriendId(userId, contactId);
            if (contactOpt.isEmpty()) {
                continue;
            }
            
            Contact contact = contactOpt.get();
            
            // 查找好友用户信息
            Optional<User> friendUserOpt = userRepository.findById(contactId);
            if (friendUserOpt.isEmpty()) {
                continue;
            }
            
            User friend = friendUserOpt.get();
            
            // 获取联系人的标签信息
            List<ContactTagResponse> tags = getContactTags(userId, contactId);
            
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
            
            contacts.add(response);
        }

        return contacts;
    }
}