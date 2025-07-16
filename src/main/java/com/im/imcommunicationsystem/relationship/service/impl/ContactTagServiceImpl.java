package com.im.imcommunicationsystem.relationship.service.impl;

import com.im.imcommunicationsystem.relationship.dto.request.ContactTagCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagUpdateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.entity.ContactTag;
import com.im.imcommunicationsystem.relationship.exception.ContactTagException;
import com.im.imcommunicationsystem.relationship.repository.ContactTagRepository;
import com.im.imcommunicationsystem.relationship.repository.ContactTagAssignmentRepository;
import com.im.imcommunicationsystem.relationship.service.ContactTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 好友标签服务实现类
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ContactTagServiceImpl implements ContactTagService {

    private final ContactTagRepository contactTagRepository;
    private final ContactTagAssignmentRepository contactTagAssignmentRepository;

    // 默认标签颜色列表
    private static final List<String> DEFAULT_COLORS = Arrays.asList(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
            "#F8C471", "#82E0AA", "#F1948A", "#85C1E9", "#D7BDE2"
    );

    @Override
    public ContactTagResponse createTag(ContactTagCreateRequest request) {
        // 验证标签名是否已存在
        if (isTagNameExists(request.getUserId(), request.getName())) {
            throw new ContactTagException("标签名称已存在: " + request.getName());
        }

        // 创建标签实体
        ContactTag contactTag = ContactTag.builder()
                .userId(request.getUserId())
                .name(request.getName().trim())
                .color(StringUtils.hasText(request.getColor()) ? request.getColor() : getRandomDefaultColor())
                .build();

        // 保存到数据库
        ContactTag savedTag = contactTagRepository.save(contactTag);

        // 转换为响应DTO
        return convertToResponse(savedTag);
    }

    @Override
    public ContactTagResponse updateTag(Long tagId, ContactTagUpdateRequest request, Long userId) {
        // 验证标签权限
        if (!validateTagPermission(tagId, userId)) {
            throw new ContactTagException("无权限操作此标签");
        }

        // 查找标签
        ContactTag contactTag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("标签不存在"));

        // 检查新名称是否与其他标签冲突
        if (StringUtils.hasText(request.getName()) && 
            !request.getName().equals(contactTag.getName()) &&
            isTagNameExists(userId, request.getName())) {
            throw new ContactTagException("标签名称已存在: " + request.getName());
        }

        // 更新标签信息
        if (StringUtils.hasText(request.getName())) {
            contactTag.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getColor())) {
            contactTag.setColor(request.getColor());
        }

        // 保存更新
        ContactTag updatedTag = contactTagRepository.save(contactTag);

        return convertToResponse(updatedTag);
    }

    @Override
    public boolean deleteTag(Long tagId, Long userId) {
        // 验证标签权限
        if (!validateTagPermission(tagId, userId)) {
            throw new ContactTagException("无权限操作此标签");
        }

        // 检查标签是否存在
        if (!contactTagRepository.existsById(tagId)) {
            throw new ContactTagException("标签不存在");
        }

        try {
            // 先删除所有相关的标签分配
            contactTagAssignmentRepository.deleteByTagId(tagId);
            
            // 删除标签
            contactTagRepository.deleteById(tagId);
            
            return true;
        } catch (Exception e) {
            throw new ContactTagException("删除标签失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactTagResponse> getUserTags(Long userId) {
        List<ContactTag> tags = contactTagRepository.findByUserId(userId);
        return tags.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContactTagResponse getTagDetail(Long tagId, Long userId) {
        // 验证标签权限
        if (!validateTagPermission(tagId, userId)) {
            throw new ContactTagException("无权限访问此标签");
        }

        ContactTag contactTag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("标签不存在"));

        return convertToResponseWithCount(contactTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactTagResponse> searchTagsByName(Long userId, String name) {
        if (!StringUtils.hasText(name)) {
            return getUserTags(userId);
        }

        List<ContactTag> tags = contactTagRepository.findByUserIdAndNameContaining(userId, name.trim());
        return tags.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactTagResponse> getTagsByColor(Long userId, String color) {
        if (!StringUtils.hasText(color)) {
            return getUserTags(userId);
        }

        List<ContactTag> tags = contactTagRepository.findByUserIdAndColor(userId, color);
        return tags.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTagNameExists(Long userId, String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return contactTagRepository.existsByUserIdAndName(userId, name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public long getTagCount(Long userId) {
        return contactTagRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactTagResponse> getRecentTags(Long userId, int limit) {
        List<ContactTag> tags = contactTagRepository.findRecentTags(userId, Math.max(1, Math.min(limit, 50)));
        return tags.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    @Override
    public int batchDeleteTags(List<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (Long tagId : tagIds) {
            try {
                if (validateTagPermission(tagId, userId)) {
                    // 删除标签分配
                    contactTagAssignmentRepository.deleteByTagId(tagId);
                    // 删除标签
                    contactTagRepository.deleteById(tagId);
                    deletedCount++;
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他标签
                // 可以考虑添加日志记录
            }
        }

        return deletedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateTagPermission(Long tagId, Long userId) {
        return contactTagRepository.findById(tagId)
                .map(tag -> tag.getUserId().equals(userId))
                .orElse(false);
    }

    @Override
    public ContactTagResponse duplicateTag(Long tagId, Long userId, String newName) {
        // 验证原标签权限
        if (!validateTagPermission(tagId, userId)) {
            throw new ContactTagException("无权限操作此标签");
        }

        // 查找原标签
        ContactTag originalTag = contactTagRepository.findById(tagId)
                .orElseThrow(() -> new ContactTagException("原标签不存在"));

        // 验证新标签名
        if (!StringUtils.hasText(newName)) {
            throw new ContactTagException("新标签名称不能为空");
        }

        if (isTagNameExists(userId, newName)) {
            throw new ContactTagException("标签名称已存在: " + newName);
        }

        // 创建新标签
        ContactTag newTag = ContactTag.builder()
                .userId(userId)
                .name(newName.trim())
                .color(originalTag.getColor())
                .build();

        ContactTag savedTag = contactTagRepository.save(newTag);
        return convertToResponse(savedTag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDefaultColors() {
        return DEFAULT_COLORS;
    }

    /**
     * 根据ID查找标签（内部方法）
     */
    @Transactional(readOnly = true)
    public Optional<ContactTag> findById(Long tagId) {
        return contactTagRepository.findById(tagId);
    }

    /**
     * 根据用户ID和名称查找标签（内部方法）
     */
    @Transactional(readOnly = true)
    public Optional<ContactTag> findByUserIdAndName(Long userId, String name) {
        return contactTagRepository.findByUserIdAndName(userId, name);
    }

    /**
     * 转换实体为响应DTO
     */
    private ContactTagResponse convertToResponse(ContactTag contactTag) {
        ContactTagResponse response = new ContactTagResponse();
        response.setTagId(contactTag.getId());
        response.setUserId(contactTag.getUserId());
        response.setName(contactTag.getName());
        response.setColor(contactTag.getColor());
        response.setCreatedAt(contactTag.getCreatedAt());
        response.setIsDefault(false);
        response.setSortOrder(0);
        return response;
    }

    /**
     * 转换实体为响应DTO（包含使用次数）
     */
    private ContactTagResponse convertToResponseWithCount(ContactTag contactTag) {
        ContactTagResponse response = convertToResponse(contactTag);
        long contactCount = contactTagAssignmentRepository.countByTagId(contactTag.getId());
        response.setContactCount((int) contactCount);
        return response;
    }

    /**
     * 获取随机默认颜色
     */
    private String getRandomDefaultColor() {
        int index = (int) (Math.random() * DEFAULT_COLORS.size());
        return DEFAULT_COLORS.get(index);
    }
}