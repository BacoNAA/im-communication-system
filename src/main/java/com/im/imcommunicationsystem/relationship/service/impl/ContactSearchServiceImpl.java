package com.im.imcommunicationsystem.relationship.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.relationship.dto.request.SearchContactRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactSearchResponse;
import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactRequest;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import com.im.imcommunicationsystem.relationship.repository.ContactRepository;
import com.im.imcommunicationsystem.relationship.repository.ContactRequestRepository;
import com.im.imcommunicationsystem.relationship.service.ContactSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 联系人搜索服务实现类
 * 实现用户搜索和好友关系状态检查功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactSearchServiceImpl implements ContactSearchService {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final ContactRequestRepository contactRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ContactSearchResponse> searchUserById(SearchContactRequest searchRequest) {
        log.info("根据用户ID搜索用户: searchRequest={}", searchRequest);
        
        if (searchRequest == null || !StringUtils.hasText(searchRequest.getKeyword()) || searchRequest.getUserId() == null) {
            log.warn("搜索参数无效: searchRequest={}", searchRequest);
            return Optional.empty();
        }
        
        String userIdStr = searchRequest.getKeyword();
        Long currentUserId = searchRequest.getUserId();
        
        // 不能搜索自己
        Optional<User> currentUser = userRepository.findById(currentUserId);
        if (currentUser.isPresent() && userIdStr.equals(currentUser.get().getUserIdStr())) {
            log.warn("用户尝试搜索自己: userIdStr={}, currentUserId={}", userIdStr, currentUserId);
            return Optional.empty();
        }
        
        Optional<User> userOptional = userRepository.findByUserIdStr(userIdStr);
        if (userOptional.isEmpty()) {
            log.info("未找到用户: userIdStr={}", userIdStr);
            return Optional.empty();
        }
        
        User user = userOptional.get();
        
        // 检查用户是否允许被搜索（这里假设所有用户都允许被搜索，实际可根据用户设置调整）
        if (!canBeSearched(user)) {
            log.info("用户不允许被搜索: userId={}", user.getId());
            return Optional.empty();
        }
        
        ContactSearchResponse response = buildContactSearchResponse(user, currentUserId);
        return Optional.of(response);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactSearchResponse> searchUserByNickname(SearchContactRequest searchRequest) {
        log.info("根据昵称搜索用户: searchRequest={}", searchRequest);
        
        if (searchRequest == null || !StringUtils.hasText(searchRequest.getKeyword()) || searchRequest.getUserId() == null) {
            log.warn("搜索参数无效: searchRequest={}", searchRequest);
            return new ArrayList<>();
        }
        
        String nickname = searchRequest.getKeyword();
        Long currentUserId = searchRequest.getUserId();
        int page = 0; // 默认页码
        int size = 10; // 默认大小
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage = userRepository.findByNicknameContainingIgnoreCase(nickname, pageable);
            
            List<ContactSearchResponse> results = new ArrayList<>();
            for (User user : userPage.getContent()) {
                // 排除自己
                if (user.getId().equals(currentUserId)) {
                    continue;
                }
                
                // 检查是否允许被搜索
                if (!canBeSearched(user)) {
                    continue;
                }
                
                // 检查搜索权限
                if (!validateSearchPermission(user.getId(), currentUserId)) {
                    continue;
                }
                
                ContactSearchResponse response = buildContactSearchResponse(user, currentUserId);
                results.add(response);
            }
            
            log.info("昵称搜索完成: nickname={}, 找到{}个结果", nickname, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("昵称搜索失败: nickname={}, error={}", nickname, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactSearchResponse> searchUsers(SearchContactRequest searchRequest) {
        log.info("通用搜索用户: searchRequest={}", searchRequest);
        
        if (searchRequest == null || !StringUtils.hasText(searchRequest.getKeyword()) || searchRequest.getUserId() == null) {
            log.warn("搜索参数无效: searchRequest={}", searchRequest);
            return new ArrayList<>();
        }
        
        List<ContactSearchResponse> results = new ArrayList<>();
        
        // 首先尝试精确匹配用户ID
        Optional<ContactSearchResponse> exactMatch = searchUserById(searchRequest);
        if (exactMatch.isPresent()) {
            results.add(exactMatch.get());
            return results;
        }
        
        // 然后尝试昵称模糊搜索
        List<ContactSearchResponse> nicknameResults = searchUserByNickname(searchRequest);
        results.addAll(nicknameResults);
        
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContactSearchResponse> searchUserByQRCode(String qrCodeContent, Long currentUserId) {
        log.info("根据二维码搜索用户: qrCodeContent={}, currentUserId={}", qrCodeContent, currentUserId);
        
        if (!StringUtils.hasText(qrCodeContent) || currentUserId == null) {
            log.warn("二维码搜索参数无效: qrCodeContent={}, currentUserId={}", qrCodeContent, currentUserId);
            return Optional.empty();
        }
        
        // 解析二维码数据，提取用户ID
        String userIdStr = extractUserIdFromQRCode(qrCodeContent);
        if (!StringUtils.hasText(userIdStr)) {
            log.warn("无法从二维码中提取用户ID: qrCodeContent={}", qrCodeContent);
            return Optional.empty();
        }
        
        // 创建新的搜索请求对象，使用提取的用户ID
        SearchContactRequest userIdSearchRequest = SearchContactRequest.builder()
                .keyword(userIdStr)
                .userId(currentUserId)
                .build();
        
        return searchUserById(userIdSearchRequest);
    }

    @Override
    public boolean validateSearchPermission(Long targetUserId, Long currentUserId) {
        log.debug("检查搜索权限: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
        
        if (currentUserId == null || targetUserId == null) {
            return false;
        }
        
        // 不能搜索自己
        if (currentUserId.equals(targetUserId)) {
            return false;
        }
        
        // 检查目标用户是否存在
        Optional<User> targetUser = userRepository.findById(targetUserId);
        if (targetUser.isEmpty()) {
            return false;
        }
        
        // 检查是否被屏蔽
        Optional<Contact> contact = contactRepository.findByUserIdAndFriendId(targetUserId, currentUserId);
        if (contact.isPresent() && contact.get().getIsBlocked()) {
            log.info("用户被目标用户屏蔽: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
            return false;
        }
        
        return canBeSearched(targetUser.get());
    }

    @Override
    @Transactional(readOnly = true)
    public String checkRelationshipStatus(Long currentUserId, Long targetUserId) {
        log.debug("获取关系状态: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
        
        if (currentUserId == null || targetUserId == null) {
            return "未知";
        }
        
        if (currentUserId.equals(targetUserId)) {
            return "自己";
        }
        
        // 检查是否为好友
        Optional<Contact> contact = contactRepository.findByUserIdAndFriendId(currentUserId, targetUserId);
        if (contact.isPresent()) {
            if (contact.get().getIsBlocked()) {
                return "已屏蔽";
            }
            return "好友";
        }
        
        // 检查是否有待处理的好友请求
        try {
            Optional<ContactRequest> sentRequest = contactRequestRepository
                    .findByRequesterIdAndRecipientIdAndStatus(currentUserId, targetUserId, ContactRequestStatus.PENDING);
            if (sentRequest.isPresent()) {
                return "已发送请求";
            }
            
            Optional<ContactRequest> receivedRequest = contactRequestRepository
                    .findByRequesterIdAndRecipientIdAndStatus(targetUserId, currentUserId, ContactRequestStatus.PENDING);
            if (receivedRequest.isPresent()) {
                return "待处理请求";
            }
        } catch (Exception e) {
            log.warn("检查好友请求状态时发生错误: currentUserId={}, targetUserId={}, error={}", currentUserId, targetUserId, e.getMessage());
            // 如果查询失败，继续执行，返回陌生人状态
        }
        
        return "陌生人";
    }

    /**
     * 构建联系人搜索响应对象
     */
    private ContactSearchResponse buildContactSearchResponse(User user, Long currentUserId) {
        String relationshipStatus = checkRelationshipStatus(currentUserId, user.getId());
        
        return ContactSearchResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .userIdString(user.getUserIdStr())
                .isFriend("好友".equals(relationshipStatus))
                .hasRequestSent("已发送请求".equals(relationshipStatus))
                .isBlocked("已屏蔽".equals(relationshipStatus))
                .relationshipStatus(relationshipStatus)
                .registeredAt(user.getCreatedAt())
                .allowSearch(canBeSearched(user))
                .build();
    }

    /**
     * 检查用户是否允许被搜索
     */
    private boolean canBeSearched(User user) {
        // 这里可以根据用户的隐私设置来判断
        // 目前默认所有用户都允许被搜索
        return true;
    }

    /**
     * 从二维码数据中提取用户ID
     */
    private String extractUserIdFromQRCode(String qrCodeData) {
        // 这里需要根据二维码的格式来解析
        // 假设二维码格式为: "user:userIdStr" 或直接是用户ID字符串
        if (qrCodeData.startsWith("user:")) {
            return qrCodeData.substring(5);
        }
        
        // 直接返回原始数据，假设就是用户ID
        return qrCodeData;
    }
}