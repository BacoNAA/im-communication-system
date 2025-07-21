package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.group.dto.request.GroupSearchRequest;
import com.im.imcommunicationsystem.group.dto.response.GroupSearchResponse;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.repository.GroupJoinRequestRepository;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.group.service.GroupSearchService;
import com.im.imcommunicationsystem.group.enums.GroupJoinRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 群组搜索服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupSearchServiceImpl implements GroupSearchService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupJoinRequestRepository joinRequestRepository;

    @Override
    public Page<GroupSearchResponse> searchGroups(GroupSearchRequest request, Long userId, Pageable pageable) {
        log.info("搜索群组: keyword={}, userId={}, page={}, size={}", 
                request.getKeyword(), userId, pageable.getPageNumber(), pageable.getPageSize());
        
        String keyword = request.getKeyword().trim();
        
        // 检查关键词是否为纯数字（可能是群组ID）
        if (Pattern.matches("^\\d+$", keyword)) {
            try {
                Long groupId = Long.parseLong(keyword);
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                
                if (groupOpt.isPresent()) {
                    GroupSearchResponse response = buildGroupSearchResponse(groupOpt.get(), userId);
                    List<GroupSearchResponse> result = new ArrayList<>();
                    result.add(response);
                    return new PageImpl<>(result, pageable, 1);
                }
            } catch (NumberFormatException e) {
                log.debug("关键词解析为群组ID时出错，尝试按名称搜索: {}", e.getMessage());
            }
        }
        
        // 按名称搜索
        Page<Group> groups = groupRepository.findByNameContaining(keyword, pageable);
        
        List<GroupSearchResponse> responses = groups.getContent().stream()
                .map(group -> buildGroupSearchResponse(group, userId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, groups.getTotalElements());
    }

    @Override
    public GroupSearchResponse getGroupById(Long groupId, Long userId) {
        log.info("根据ID获取群组: groupId={}, userId={}", groupId, userId);
        
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        
        if (groupOpt.isPresent()) {
            return buildGroupSearchResponse(groupOpt.get(), userId);
        }
        
        return null;
    }
    
    /**
     * 构建群组搜索响应
     */
    private GroupSearchResponse buildGroupSearchResponse(Group group, Long userId) {
        // 检查用户是否是群成员
        boolean isMember = false;
        try {
            // 使用查询方法检查用户是否为群成员
            isMember = groupMemberRepository.findByIdGroupIdAndIdUserId(group.getId(), userId).isPresent();
        } catch (Exception e) {
            log.warn("检查用户是否为群成员时出错: {}", e.getMessage());
        }
        
        // 检查用户是否有待处理的加入请求
        boolean hasPendingRequest = joinRequestRepository.existsByGroupIdAndUserIdAndStatus(
                group.getId(), userId, GroupJoinRequestStatus.PENDING);
        
        // 获取群主信息
        String ownerName = "未知用户";
        Optional<User> ownerOpt = userRepository.findById(group.getOwnerId());
        if (ownerOpt.isPresent()) {
            User owner = ownerOpt.get();
            if (owner.getNickname() != null) {
                ownerName = owner.getNickname();
            } else {
                ownerName = owner.getEmail();
            }
        }
        
        // 获取成员数量
        long memberCountLong = groupMemberRepository.countByIdGroupId(group.getId());
        Integer memberCount = (int) memberCountLong;
        
        return GroupSearchResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .avatarUrl(group.getAvatarUrl())
                .description(group.getDescription())
                .ownerId(group.getOwnerId())
                .ownerName(ownerName)
                .memberCount(memberCount)
                .requiresApproval(group.getRequiresApproval())
                .createdAt(group.getCreatedAt())
                .isMember(isMember)
                .hasPendingRequest(hasPendingRequest)
                .build();
    }
} 