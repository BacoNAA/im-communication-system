package com.im.imcommunicationsystem.relationship.service.impl;

import com.im.imcommunicationsystem.relationship.dto.request.ContactRequestCreateRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactRequestResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactRequestStatsResponse;
import com.im.imcommunicationsystem.relationship.entity.Contact;
import com.im.imcommunicationsystem.relationship.entity.ContactId;
import com.im.imcommunicationsystem.relationship.entity.ContactRequest;
import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import com.im.imcommunicationsystem.relationship.exception.ContactRequestException;
import com.im.imcommunicationsystem.relationship.repository.ContactRepository;
import com.im.imcommunicationsystem.relationship.repository.ContactRequestRepository;
import com.im.imcommunicationsystem.relationship.service.ContactRequestService;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 好友请求服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestServiceImpl implements ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean sendContactRequest(ContactRequestCreateRequest request) {
        log.info("发送好友请求: requesterId={}, recipientId={}", request.getRequesterId(), request.getRecipientId());
        
        // 验证请求参数
        if (request.getRequesterId() == null || request.getRecipientId() == null) {
            throw new ContactRequestException("请求者ID和接收者ID不能为空");
        }
        
        if (request.getRequesterId().equals(request.getRecipientId())) {
            throw new ContactRequestException("不能向自己发送好友请求");
        }
        
        // 检查用户是否存在
        if (!userRepository.existsById(request.getRequesterId()) || !userRepository.existsById(request.getRecipientId())) {
            throw new ContactRequestException("用户不存在");
        }
        
        // 检查是否已经是好友
        ContactId contactId = new ContactId(request.getRequesterId(), request.getRecipientId());
        if (contactRepository.existsById(contactId)) {
            throw new ContactRequestException("已经是好友关系");
        }
        
        // 检查是否已有待处理的请求
        if (contactRequestRepository.existsByRequesterIdAndRecipientIdAndStatus(
                request.getRequesterId(), request.getRecipientId(), ContactRequestStatus.PENDING)) {
            throw new ContactRequestException("已存在待处理的好友请求");
        }
        
        // 创建好友请求
        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setRequesterId(request.getRequesterId());
        contactRequest.setRecipientId(request.getRecipientId());
        contactRequest.setMessage(StringUtils.hasText(request.getVerificationMessage()) ? 
                request.getVerificationMessage() : "你好，我想添加你为好友");
        contactRequest.setStatus(ContactRequestStatus.PENDING);
        contactRequest.setCreatedAt(LocalDateTime.now());
        
        contactRequestRepository.save(contactRequest);
        log.info("好友请求发送成功: requestId={}", contactRequest.getId());
        
        return true;
    }

    @Override
    @Transactional
    public boolean acceptContactRequest(Long requestId, Long recipientId) {
        log.info("接受好友请求: requestId={}, recipientId={}", requestId, recipientId);
        
        // 查找好友请求
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new ContactRequestException("好友请求不存在");
        }
        
        ContactRequest contactRequest = requestOpt.get();
        
        // 验证权限
        if (!contactRequest.getRecipientId().equals(recipientId)) {
            throw new ContactRequestException("无权限处理此好友请求");
        }
        
        // 检查请求状态
        if (contactRequest.getStatus() != ContactRequestStatus.PENDING) {
            throw new ContactRequestException("请求已被处理");
        }
        
        // 检查是否已经是好友
        ContactId contactId = new ContactId(contactRequest.getRequesterId(), contactRequest.getRecipientId());
        if (contactRepository.existsById(contactId)) {
            throw new ContactRequestException("已经是好友关系");
        }
        
        // 更新请求状态
        contactRequest.setStatus(ContactRequestStatus.ACCEPTED);
        contactRequest.setHandledAt(LocalDateTime.now());
        contactRequestRepository.save(contactRequest);
        
        // 创建双向好友关系
        Contact contact1 = new Contact();
        contact1.setUserId(contactRequest.getRequesterId());
        contact1.setFriendId(contactRequest.getRecipientId());
        
        Contact contact2 = new Contact();
        contact2.setUserId(contactRequest.getRecipientId());
        contact2.setFriendId(contactRequest.getRequesterId());
        
        contactRepository.save(contact1);
        contactRepository.save(contact2);
        
        log.info("好友请求接受成功，已建立好友关系: requestId={}", requestId);
        return true;
    }

    @Override
    @Transactional
    public boolean rejectContactRequest(Long requestId, Long recipientId) {
        log.info("拒绝好友请求: requestId={}, recipientId={}", requestId, recipientId);
        
        // 查找好友请求
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new ContactRequestException("好友请求不存在");
        }
        
        ContactRequest contactRequest = requestOpt.get();
        
        // 验证权限
        if (!contactRequest.getRecipientId().equals(recipientId)) {
            throw new ContactRequestException("无权限处理此好友请求");
        }
        
        // 检查请求状态
        if (contactRequest.getStatus() != ContactRequestStatus.PENDING) {
            throw new ContactRequestException("请求已被处理");
        }
        
        // 更新请求状态
        contactRequest.setStatus(ContactRequestStatus.REJECTED);
        contactRequest.setHandledAt(LocalDateTime.now());
        contactRequestRepository.save(contactRequest);
        
        log.info("好友请求拒绝成功: requestId={}", requestId);
        return true;
    }

    @Override
    @Transactional
    public boolean withdrawContactRequest(Long requestId, Long requesterId) {
        log.info("撤回好友请求: requestId={}, requesterId={}", requestId, requesterId);
        
        // 查找好友请求
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new ContactRequestException("好友请求不存在");
        }
        
        ContactRequest contactRequest = requestOpt.get();
        
        // 验证权限
        if (!contactRequest.getRequesterId().equals(requesterId)) {
            throw new ContactRequestException("无权限撤回此好友请求");
        }
        
        // 检查请求状态
        if (contactRequest.getStatus() != ContactRequestStatus.PENDING) {
            throw new ContactRequestException("只能撤回待处理的请求");
        }
        
        // 删除请求记录
        contactRequestRepository.delete(contactRequest);
        
        log.info("好友请求撤回成功: requestId={}", requestId);
        return true;
    }

    @Override
    public List<ContactRequestResponse> getReceivedRequests(Long userId, ContactRequestStatus status) {
        log.info("获取收到的好友请求列表: userId={}, status={}", userId, status);
        
        List<ContactRequest> requests;
        if (status != null) {
            requests = contactRequestRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            requests = contactRequestRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        }
        
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContactRequestResponse> getSentRequests(Long userId, ContactRequestStatus status) {
        log.info("获取发送的好友请求列表: userId={}, status={}", userId, status);
        
        List<ContactRequest> requests;
        if (status != null) {
            requests = contactRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            requests = contactRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
        }
        
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ContactRequestResponse getRequestDetail(Long requestId, Long userId) {
        log.info("获取好友请求详情: requestId={}, userId={}", requestId, userId);
        
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new ContactRequestException("好友请求不存在");
        }
        
        ContactRequest contactRequest = requestOpt.get();
        
        // 验证权限（只有请求者或接收者可以查看详情）
        if (!contactRequest.getRequesterId().equals(userId) && !contactRequest.getRecipientId().equals(userId)) {
            throw new ContactRequestException("无权限查看此好友请求");
        }
        
        return convertToResponse(contactRequest);
    }

    @Override
    public boolean hasPendingRequest(Long requesterId, Long recipientId) {
        return contactRequestRepository.existsByRequesterIdAndRecipientIdAndStatus(
                requesterId, recipientId, ContactRequestStatus.PENDING);
    }

    @Override
    public long getPendingRequestCount(Long userId) {
        return contactRequestRepository.countByRecipientIdAndStatus(userId, ContactRequestStatus.PENDING);
    }

    @Override
    @Transactional
    public int batchProcessRequests(List<Long> requestIds, Long userId, boolean accept) {
        log.info("批量处理好友请求: requestIds={}, userId={}, accept={}", requestIds, userId, accept);
        
        if (requestIds == null || requestIds.isEmpty()) {
            return 0;
        }
        
        int processedCount = 0;
        for (Long requestId : requestIds) {
            try {
                if (accept) {
                    acceptContactRequest(requestId, userId);
                } else {
                    rejectContactRequest(requestId, userId);
                }
                processedCount++;
            } catch (Exception e) {
                log.warn("批量处理好友请求失败: requestId={}, error={}", requestId, e.getMessage());
            }
        }
        
        log.info("批量处理完成: 总数={}, 成功={}", requestIds.size(), processedCount);
        return processedCount;
    }

    @Override
    @Transactional
    public boolean deleteProcessedRequest(Long requestId, Long userId) {
        log.info("删除已处理的请求记录: requestId={}, userId={}", requestId, userId);
        
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new ContactRequestException("好友请求不存在");
        }
        
        ContactRequest contactRequest = requestOpt.get();
        
        // 验证权限
        if (!contactRequest.getRequesterId().equals(userId) && !contactRequest.getRecipientId().equals(userId)) {
            throw new ContactRequestException("无权限删除此好友请求");
        }
        
        // 只能删除已处理的请求
        if (contactRequest.getStatus() == ContactRequestStatus.PENDING) {
            throw new ContactRequestException("不能删除待处理的请求");
        }
        
        contactRequestRepository.delete(contactRequest);
        log.info("请求记录删除成功: requestId={}", requestId);
        return true;
    }

    @Override
    @Transactional
    public int cleanupExpiredRequests(int expireDays) {
        log.info("清理过期的好友请求: expireDays={}", expireDays);
        
        LocalDateTime expireTime = LocalDateTime.now().minusDays(expireDays);
        int deletedCount = contactRequestRepository.deleteByCreatedAtBeforeAndStatus(expireTime, ContactRequestStatus.PENDING);
        
        log.info("清理过期请求完成: 删除数量={}", deletedCount);
        return deletedCount;
    }

    @Override
    public List<ContactRequestResponse> getRecentRequests(Long userId, int limit) {
        log.info("获取最近的好友请求: userId={}, limit={}", userId, limit);
        
        List<ContactRequest> requests = contactRequestRepository.findRecentRequestsByUserId(userId, limit);
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean validateRequestPermission(Long requestId, Long userId) {
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            return false;
        }
        
        ContactRequest contactRequest = requestOpt.get();
         return contactRequest.getRequesterId().equals(userId) || contactRequest.getRecipientId().equals(userId);
     }
     
     @Override
     public ContactRequestStatsResponse getRequestStats(Long userId) {
         log.info("获取好友请求统计信息: userId={}", userId);
         
         ContactRequestStatsResponse stats = new ContactRequestStatsResponse();
         
         // 收到的待处理请求数量
         stats.setReceivedPendingCount(contactRequestRepository.countByRecipientIdAndStatus(userId, ContactRequestStatus.PENDING));
         
         // 发送的待处理请求数量
         stats.setSentPendingCount(contactRequestRepository.countByRequesterIdAndStatus(userId, ContactRequestStatus.PENDING));
         
         // 收到的总请求数量
         stats.setReceivedTotalCount(contactRequestRepository.countByRecipientId(userId));
         
         // 发送的总请求数量
         stats.setSentTotalCount(contactRequestRepository.countByRequesterId(userId));
         
         // 今日收到的请求数量
         LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
         LocalDateTime todayEnd = todayStart.plusDays(1);
         stats.setTodayReceivedCount(contactRequestRepository.countByRecipientIdAndCreatedAtBetween(userId, todayStart, todayEnd));
         
         // 今日发送的请求数量
         stats.setTodaySentCount(contactRequestRepository.countByRequesterIdAndCreatedAtBetween(userId, todayStart, todayEnd));
         
         // 已同意的请求数量（收到的）
         stats.setAcceptedReceivedCount(contactRequestRepository.countByRecipientIdAndStatus(userId, ContactRequestStatus.ACCEPTED));
         
         // 已同意的请求数量（发送的）
         stats.setAcceptedSentCount(contactRequestRepository.countByRequesterIdAndStatus(userId, ContactRequestStatus.ACCEPTED));
         
         log.info("统计信息获取完成: receivedPending={}, sentPending={}, acceptedReceived={}, acceptedSent={}", 
                 stats.getReceivedPendingCount(), stats.getSentPendingCount(), 
                 stats.getAcceptedReceivedCount(), stats.getAcceptedSentCount());
         return stats;
     }
     
     /**
      * 将ContactRequest实体转换为ContactRequestResponse DTO
      */
     private ContactRequestResponse convertToResponse(ContactRequest request) {
         ContactRequestResponse response = new ContactRequestResponse();
         response.setRequestId(request.getId());
         response.setRequesterId(request.getRequesterId());
         response.setRecipientId(request.getRecipientId());
         response.setVerificationMessage(request.getMessage());
         response.setStatus(request.getStatus());
         response.setStatusDescription(request.getStatus().getDescription());
         response.setCreatedAt(request.getCreatedAt());
         response.setProcessedAt(request.getHandledAt());
         
         // 获取请求者信息
         Optional<User> requesterOpt = userRepository.findById(request.getRequesterId());
         if (requesterOpt.isPresent()) {
             User requester = requesterOpt.get();
             response.setRequesterUsername(requester.getEmail());
             response.setRequesterNickname(requester.getNickname());
             response.setRequesterAvatarUrl(requester.getAvatarUrl());
             response.setRequesterUserIdStr(requester.getUserIdStr());
         }
         
         // 获取接收者信息
         Optional<User> recipientOpt = userRepository.findById(request.getRecipientId());
         if (recipientOpt.isPresent()) {
             User recipient = recipientOpt.get();
             response.setRecipientUsername(recipient.getEmail());
             response.setRecipientNickname(recipient.getNickname());
             response.setRecipientAvatarUrl(recipient.getAvatarUrl());
             response.setRecipientUserIdStr(recipient.getUserIdStr());
         }
         
         // 设置操作权限
         response.setCanProcess(request.getStatus() == ContactRequestStatus.PENDING);
         response.setCanWithdraw(request.getStatus() == ContactRequestStatus.PENDING);
         
         return response;
     }
 }