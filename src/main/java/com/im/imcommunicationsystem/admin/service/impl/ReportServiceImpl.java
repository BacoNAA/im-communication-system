package com.im.imcommunicationsystem.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.admin.dto.request.CreateReportRequest;
import com.im.imcommunicationsystem.admin.dto.request.ReportHandleRequest;
import com.im.imcommunicationsystem.admin.dto.response.ReportResponse;
import com.im.imcommunicationsystem.admin.entity.AdminOperationLog;
import com.im.imcommunicationsystem.admin.entity.Report;
import com.im.imcommunicationsystem.admin.repository.AdminOperationLogRepository;
import com.im.imcommunicationsystem.admin.repository.ReportRepository;
import com.im.imcommunicationsystem.admin.service.ReportService;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.common.service.WebSocketService;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.message.entity.Message;
import com.im.imcommunicationsystem.message.enums.MessageStatus;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.moment.entity.Moment;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 举报服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AdminOperationLogRepository adminOperationLogRepository;
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MomentRepository momentRepository;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    @Override
    public Map<String, Object> getReportedContentDetails(String contentType, Long contentId) {
        log.info("获取被举报内容详情，内容类型：{}，内容ID：{}", contentType, contentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("contentType", contentType);
        result.put("contentId", contentId);
        
        try {
            // 如果是GROUP_MEMBER类型，需要获取完整的举报信息以获取reportedUserId
            if ("GROUP_MEMBER".equals(contentType)) {
                // 查找对应的举报记录
                List<Report> reports = reportRepository.findByReportedContentTypeAndReportedContentId(contentType, contentId);
                if (!reports.isEmpty()) {
                    Report report = reports.get(0); // 取最近的一条
                    Map<String, Object> reportContext = new HashMap<>();
                    reportContext.put("reportedUserId", report.getReportedUserId());
                    result.put("reportContext", reportContext);
                }
            }
            
            switch (contentType) {
                case "USER":
                    fetchUserDetails(contentId, result);
                    break;
                case "MESSAGE":
                    fetchMessageDetails(contentId, result);
                    break;
                case "GROUP":
                    fetchGroupDetails(contentId, result);
                    break;
                case "GROUP_MEMBER":
                    fetchGroupMemberDetails(contentId, result);
                    break;
                case "MOMENT":
                    fetchMomentDetails(contentId, result);
                    break;
                default:
                    result.put("errorMsg", "不支持的内容类型：" + contentType);
                    break;
            }
        } catch (Exception e) {
            log.error("获取被举报内容详情失败，内容类型：{}，内容ID：{}", contentType, contentId, e);
            result.put("errorMsg", "获取内容详情失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取用户详情
     */
    private void fetchUserDetails(Long userId, Map<String, Object> result) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("id", user.getId());
        userDetails.put("nickname", user.getNickname());
        userDetails.put("email", user.getEmail());
        userDetails.put("createdAt", user.getCreatedAt());
        
        result.put("content", userDetails);
    }
    
    /**
     * 获取消息详情
     */
    private void fetchMessageDetails(Long messageId, Map<String, Object> result) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        
        Map<String, Object> messageDetails = new HashMap<>();
        messageDetails.put("id", message.getId());
        messageDetails.put("content", message.getContent());
        messageDetails.put("senderId", message.getSenderId());
        messageDetails.put("senderName", sender != null ? sender.getNickname() : "未知用户");
        messageDetails.put("conversationId", message.getConversationId());
        messageDetails.put("createdAt", message.getCreatedAt());
        
        // 消息类型，使用messageType字段而不是type
        if (message.getMessageType() != null) {
            messageDetails.put("type", message.getMessageType().toString());
        }
        
        if (message.getMediaFileId() != null) {
            messageDetails.put("mediaFileId", message.getMediaFileId());
        }
        
        result.put("content", messageDetails);
    }
    
    /**
     * 获取群组详情
     */
    private void fetchGroupDetails(Long groupId, Map<String, Object> result) {
        // 尝试获取群组信息
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        
        Map<String, Object> groupDetails = new HashMap<>();
        
        if (groupOpt.isPresent()) {
            // 群组存在
            Group group = groupOpt.get();
        User owner = userRepository.findById(group.getOwnerId()).orElse(null);
        
        groupDetails.put("id", group.getId());
        groupDetails.put("name", group.getName());
        groupDetails.put("description", group.getDescription());
        groupDetails.put("ownerId", group.getOwnerId());
        groupDetails.put("ownerName", owner != null ? owner.getNickname() : "未知用户");
        groupDetails.put("createdAt", group.getCreatedAt());
            
            // 检查封禁状态
            if (group.getIsBanned() != null && group.getIsBanned()) {
                groupDetails.put("isBanned", true);
                groupDetails.put("bannedReason", group.getBannedReason());
                groupDetails.put("bannedUntil", group.getBannedUntil());
            }
        
        // 使用avatarUrl而不是avatar
        if (group.getAvatarUrl() != null) {
            groupDetails.put("avatar", group.getAvatarUrl());
        }
        
        // 群成员数量需要单独查询
        try {
            long memberCount = groupMemberRepository.countByIdGroupId(groupId);
            groupDetails.put("memberCount", memberCount);
        } catch (Exception e) {
            log.warn("获取群组成员数量失败", e);
            groupDetails.put("memberCount", 0);
            }
        } else {
            // 群组不存在，已被删除
            groupDetails.put("id", groupId);
            groupDetails.put("status", "deleted");
            groupDetails.put("message", "该群组已被删除或解散");
        }
        
        result.put("content", groupDetails);
    }
    
    /**
     * 获取群成员详情
     * 针对GROUP_MEMBER类型的举报，contentId是群组ID而非成员ID
     */
    private void fetchGroupMemberDetails(Long groupId, Map<String, Object> result) {
            // 获取被举报的成员信息 - 通常需要从举报中获取reportedUserId
            Long reportedUserId = null;
            
            if (result.containsKey("reportContext") && result.get("reportContext") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reportContext = (Map<String, Object>) result.get("reportContext");
                if (reportContext.containsKey("reportedUserId")) {
                    reportedUserId = Long.valueOf(reportContext.get("reportedUserId").toString());
                }
            }
            
            Map<String, Object> memberDetails = new HashMap<>();
            memberDetails.put("groupId", groupId);
        
        // 尝试获取群组信息
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        
        if (groupOpt.isPresent()) {
            // 群组存在
            Group group = groupOpt.get();
            memberDetails.put("groupName", group.getName());
            
            // 检查群组封禁状态
            if (group.getIsBanned() != null && group.getIsBanned()) {
                memberDetails.put("groupStatus", "banned");
                memberDetails.put("groupBannedReason", group.getBannedReason());
                if (group.getBannedUntil() != null) {
                    memberDetails.put("groupBannedUntil", group.getBannedUntil());
                }
            }
            
            // 如果有被举报用户ID，则添加用户信息
            if (reportedUserId != null) {
                try {
                    // 尝试获取成员关系
                    Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, reportedUserId);
                    
                    if (memberOpt.isPresent()) {
                        GroupMember groupMember = memberOpt.get();
                        User member = userRepository.findById(reportedUserId).orElse(null);
                        
                        memberDetails.put("userId", reportedUserId);
                        memberDetails.put("nickname", member != null ? member.getNickname() : "未知用户");
                        memberDetails.put("role", groupMember.getRole());
                        memberDetails.put("joinTime", groupMember.getJoinedAt());
                    } else {
                        memberDetails.put("userId", reportedUserId);
                        memberDetails.put("userNotFound", "该用户不在此群组中或已退出");
                        
                        User user = userRepository.findById(reportedUserId).orElse(null);
                        if (user != null) {
                            memberDetails.put("nickname", user.getNickname());
                        } else {
                            memberDetails.put("nickname", "未知用户");
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取群成员详细信息失败", e);
                    memberDetails.put("userId", reportedUserId);
                    memberDetails.put("error", "获取成员信息失败: " + e.getMessage());
                }
            } else {
                memberDetails.put("warning", "无法获取被举报用户信息");
            }
        } else {
            // 群组不存在
            memberDetails.put("groupStatus", "deleted");
            memberDetails.put("message", "该群组已被删除或解散");
            
            // 如果有被举报用户信息，仍然展示
            if (reportedUserId != null) {
                User user = userRepository.findById(reportedUserId).orElse(null);
                if (user != null) {
                    memberDetails.put("userId", reportedUserId);
                    memberDetails.put("nickname", user.getNickname());
                    memberDetails.put("note", "用户所在群组已被删除");
                } else {
                    memberDetails.put("userId", reportedUserId);
                    memberDetails.put("nickname", "未知用户");
        }
            }
        }
        
        result.put("content", memberDetails);
    }
    
    /**
     * 获取动态详情
     */
    private void fetchMomentDetails(Long momentId, Map<String, Object> result) {
        // 尝试获取动态信息
        Optional<Moment> momentOpt = momentRepository.findById(momentId);
        
        Map<String, Object> momentDetails = new HashMap<>();
        momentDetails.put("id", momentId);
        
        if (momentOpt.isPresent()) {
            // 动态存在
            Moment moment = momentOpt.get();
        User author = userRepository.findById(moment.getUserId()).orElse(null);
        
        momentDetails.put("userId", moment.getUserId());
        momentDetails.put("userNickname", author != null ? author.getNickname() : "未知用户");
        momentDetails.put("content", moment.getContent());
        momentDetails.put("visibility", moment.getVisibilityType().toString());
        momentDetails.put("likeCount", moment.getLikeCount());
        momentDetails.put("commentCount", moment.getCommentCount());
        momentDetails.put("createdAt", moment.getCreatedAt());
        
        // 动态媒体处理
        try {
            if (moment.getMediaUrls() != null) {
                // 将JSON字符串解析为媒体列表
                List<Map<String, Object>> mediaList = objectMapper.readValue(
                    moment.getMediaUrls(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
                momentDetails.put("media", mediaList);
            }
        } catch (Exception e) {
            log.warn("解析动态媒体信息失败", e);
            }
        } else {
            // 动态不存在，已被删除
            momentDetails.put("status", "deleted");
            momentDetails.put("message", "该动态已被删除");
        }
        
        result.put("content", momentDetails);
    }
    
    @Override
    public Page<ReportResponse> getReportListWithPagination(Pageable pageable, String status, String contentType) {
        log.info("获取举报列表，状态：{}，内容类型：{}", status, contentType);
        
        Page<Report> reportPage;
        
        // 根据过滤条件查询
        if (StringUtils.hasText(status) && StringUtils.hasText(contentType)) {
            // 状态和内容类型都有指定
            try {
                Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toLowerCase());
                reportPage = reportRepository.findByStatusAndReportedContentType(reportStatus, contentType, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("无效的举报状态：{}", status);
                reportPage = reportRepository.findByReportedContentType(contentType, pageable);
            }
        } else if (StringUtils.hasText(status)) {
            // 只有状态有指定
            try {
                Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toLowerCase());
                reportPage = reportRepository.findByStatus(reportStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("无效的举报状态：{}", status);
                reportPage = reportRepository.findAll(pageable);
            }
        } else if (StringUtils.hasText(contentType)) {
            // 只有内容类型有指定
            reportPage = reportRepository.findByReportedContentType(contentType, pageable);
        } else {
            // 没有任何过滤条件
            reportPage = reportRepository.findAll(pageable);
        }
        
        // 获取所有举报者和被举报者ID
        List<Long> userIds = new ArrayList<>();
        reportPage.getContent().forEach(report -> {
            userIds.add(report.getReporterId());
            if (report.getReportedUserId() != null) {
                userIds.add(report.getReportedUserId());
            }
        });
        
        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(user -> userMap.put(user.getId(), user));
        }
        
        // 转换为DTO
        List<ReportResponse> responseList = reportPage.getContent().stream()
                .map(report -> convertToReportResponse(report, userMap))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responseList, pageable, reportPage.getTotalElements());
    }

    @Override
    public ReportResponse getReportDetails(Long reportId) {
        log.info("获取举报详情，ID：{}", reportId);
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("举报不存在"));
        
        // 获取用户信息
        Map<Long, User> userMap = new HashMap<>();
        
        // 获取举报者信息
        Optional<User> reporterOpt = userRepository.findById(report.getReporterId());
        reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
        
        // 获取被举报者信息（如果有）
        if (report.getReportedUserId() != null) {
            Optional<User> reportedUserOpt = userRepository.findById(report.getReportedUserId());
            reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
        }
        
        return convertToReportResponse(report, userMap);
    }

    @Override
    @Transactional
    public ReportResponse handleReport(Long adminId, ReportHandleRequest request) {
        log.info("开始处理举报，管理员ID：{}，请求：{}", adminId, request);
        
        // 验证请求
        if (request == null || request.getReportId() == null) {
            log.error("请求无效: request={}", request);
            throw new BusinessException("请求无效");
        }
        
        try {
            // 获取举报
            Report report = reportRepository.findById(request.getReportId())
                    .orElseThrow(() -> {
                        log.error("举报不存在: reportId={}", request.getReportId());
                        return new BusinessException("举报不存在");
                    });
            
            // 检查举报是否已处理完成（已解决或已拒绝）
            if (report.getStatus() == Report.ReportStatus.resolved || report.getStatus() == Report.ReportStatus.rejected) {
                log.warn("举报已处理完成，无法重复处理: reportId={}, status={}", report.getId(), report.getStatus());
                throw new BusinessException("举报已处理完成（已解决或已拒绝），无法重复处理");
            }
            
            // 更新举报状态
            Report.ReportStatus newStatus;
            
            switch (request.getAction().toLowerCase()) {
                case "process":
                    newStatus = Report.ReportStatus.processing;
                    // 只有待处理的举报才能变为处理中
                    if (report.getStatus() != Report.ReportStatus.pending) {
                        log.warn("只有待处理的举报才能变为处理中: reportId={}, currentStatus={}", 
                                 report.getId(), report.getStatus());
                        throw new BusinessException("只有待处理的举报才能变为处理中状态");
                    }
                    break;
                case "resolve":
                    newStatus = Report.ReportStatus.resolved;
                    // 待处理或处理中的举报都可以直接解决
                    if (report.getStatus() != Report.ReportStatus.pending && 
                        report.getStatus() != Report.ReportStatus.processing) {
                        log.warn("只有待处理或处理中的举报才能标记为已解决: reportId={}, currentStatus={}", 
                                 report.getId(), report.getStatus());
                        throw new BusinessException("只有待处理或处理中的举报才能标记为已解决");
                    }
                    break;
                case "reject":
                    newStatus = Report.ReportStatus.rejected;
                    // 待处理或处理中的举报都可以拒绝
                    if (report.getStatus() != Report.ReportStatus.pending && 
                        report.getStatus() != Report.ReportStatus.processing) {
                        log.warn("只有待处理或处理中的举报才能拒绝: reportId={}, currentStatus={}", 
                                 report.getId(), report.getStatus());
                        throw new BusinessException("只有待处理或处理中的举报才能拒绝");
                    }
                    break;
                default:
                    log.error("无效的操作类型: action={}", request.getAction());
                    throw new BusinessException("无效的操作类型");
            }
            
            log.info("更新举报状态: reportId={}, oldStatus={}, newStatus={}", report.getId(), report.getStatus(), newStatus);
            report.setStatus(newStatus);
            report.setHandledAt(LocalDateTime.now());
            report.setHandledBy(adminId);
            
            // 根据举报类型和请求的操作执行相应的动作
            try {
                processReportActions(report, request, adminId);
            } catch (Exception e) {
                log.error("执行举报处理操作失败: reportId={}, error={}", report.getId(), e.getMessage(), e);
                // 记录失败但继续执行后续流程
                // 可以选择在操作日志中记录失败原因
            }
            
            // 保存举报
            Report updatedReport = reportRepository.save(report);
            log.info("举报保存成功: reportId={}", updatedReport.getId());
            
            // 记录操作日志
            try {
                recordOperationLog(adminId, report, request);
                log.info("操作日志记录成功: reportId={}, adminId={}", report.getId(), adminId);
            } catch (Exception e) {
                log.error("记录操作日志失败: reportId={}, adminId={}, error={}", report.getId(), adminId, e.getMessage(), e);
                // 操作日志记录失败不影响主流程
            }
            
            // 获取用户信息以构建响应
            Map<Long, User> userMap = new HashMap<>();
            
            Optional<User> reporterOpt = userRepository.findById(updatedReport.getReporterId());
            reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
            
            if (updatedReport.getReportedUserId() != null) {
                Optional<User> reportedUserOpt = userRepository.findById(updatedReport.getReportedUserId());
                reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
            }
            
            ReportResponse response = convertToReportResponse(updatedReport, userMap);
            log.info("举报处理成功完成: reportId={}, adminId={}", report.getId(), adminId);
            return response;
        } catch (BusinessException be) {
            // 已知业务异常直接抛出
            throw be;
        } catch (Exception e) {
            // 未知异常记录详细日志并转为业务异常
            log.error("处理举报时发生未知错误: adminId={}, request={}, error={}", adminId, request, e.getMessage(), e);
            throw new BusinessException("处理举报失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录操作日志
     */
    private void recordOperationLog(Long adminId, Report report, ReportHandleRequest request) {
        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setAdminId(adminId);
        operationLog.setOperationTypeString("HANDLE_REPORT");
        operationLog.setTargetTypeString("REPORT");
        operationLog.setTargetId(report.getId());
        
        // 构建详细的操作描述，包含用户和内容操作信息
        StringBuilder descBuilder = new StringBuilder();
        descBuilder.append(String.format("处理举报 #%d: %s，操作: %s，结果: %s", 
                report.getId(), report.getReason(), request.getAction(), request.getResult()));
        
        if (request.getUserAction() != null && !"none".equals(request.getUserAction())) {
            descBuilder.append(String.format("，用户操作: %s", request.getUserAction()));
            if ("temporary_ban".equals(request.getUserAction()) && request.getBanDuration() != null) {
                descBuilder.append(String.format("，封禁时长: %d小时", request.getBanDuration()));
            }
        }
        
        if (request.getContentAction() != null && !"none".equals(request.getContentAction())) {
            descBuilder.append(String.format("，内容操作: %s", request.getContentAction()));
        }
        
        operationLog.setDescription(descBuilder.toString());
        operationLog.setCreatedAt(LocalDateTime.now());
        
        adminOperationLogRepository.save(operationLog);
    }

    /**
     * 根据举报类型和请求的操作执行相应的动作
     *
     * @param report 举报
     * @param request 处理请求
     * @param adminId 管理员ID
     */
    private void processReportActions(Report report, ReportHandleRequest request, Long adminId) {
        // 获取必要的信息
        String reportedContentType = report.getReportedContentType();
        Long reportedContentId = report.getReportedContentId();
        Long reportedUserId = report.getReportedUserId();
        String userAction = request.getUserAction();
        String contentAction = request.getContentAction();
        Integer banDuration = request.getBanDuration();

        try {
            // 特殊处理群组成员类型举报
            if ("GROUP_MEMBER".equals(reportedContentType)) {
                // 处理对群成员的操作
                if (userAction != null && !"none".equals(userAction)) {
                    processGroupMemberAction(reportedContentId, reportedUserId, userAction, request.getResult(), adminId);
                    log.info("已处理群组成员操作: 群组ID={}, 用户ID={}, 操作={}", 
                             reportedContentId, reportedUserId, userAction);
                }
                
                // 处理对群组的操作
                if (contentAction != null && !"none".equals(contentAction)) {
                    processContentAction(reportedContentType, reportedContentId, contentAction, 
                                        banDuration, request.getResult(), adminId);
                }
            } else {
            // 处理对用户的操作
            if (userAction != null && !"none".equals(userAction) && reportedUserId != null) {
                processUserAction(reportedUserId, userAction, banDuration, request.getResult(), adminId);
            }

            // 处理对内容的操作
            if (contentAction != null && !"none".equals(contentAction)) {
                    processContentAction(reportedContentType, reportedContentId, contentAction, 
                                        banDuration, request.getResult(), adminId);
                }
            }
        } catch (Exception e) {
            log.error("执行举报处理操作失败", e);
            throw new BusinessException("执行操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理对用户的操作
     *
     * @param userId 用户ID
     * @param action 操作
     * @param banDuration 封禁时长（小时）
     * @param reason 原因
     * @param adminId 管理员ID
     */
    private void processUserAction(Long userId, String action, Integer banDuration, String reason, Long adminId) {
        // 检查用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        switch (action) {
            case "warn":
                // 发送警告通知给用户，可以通过系统通知或消息实现
                log.info("向用户 {} 发送警告: {}", userId, reason);
                // TODO: 实现发送警告的逻辑
                break;
                
            case "temporary_ban":
                // 临时封禁用户
                if (banDuration == null || banDuration <= 0) {
                    throw new BusinessException("封禁时长必须大于0");
                }
                
                log.info("临时封禁用户 {}，时长: {} 小时，原因: {}", userId, banDuration, reason);
                // 计算封禁截止时间
                LocalDateTime bannedUntil = LocalDateTime.now().plusHours(banDuration);
                
                // 设置用户封禁状态
                user.setIsBanned(true);
                user.setBannedUntil(bannedUntil);
                user.setBannedReason(reason);
                userRepository.save(user);
                
                // 记录管理员操作
                logAdminAction(adminId, "BAN_USER_TEMPORARY", userId, "USER", 
                              String.format("临时封禁用户，时长: %d小时，原因: %s", banDuration, reason));
                
                log.info("用户 {} 已被临时封禁，截止时间: {}", userId, bannedUntil);
                break;
                
            case "permanent_ban":
                // 永久封禁用户
                log.info("永久封禁用户 {}，原因: {}", userId, reason);
                
                // 设置用户封禁状态（永久封禁不设置截止时间）
                user.setIsBanned(true);
                user.setBannedUntil(null);
                user.setBannedReason(reason);
                userRepository.save(user);
                
                // 记录管理员操作
                logAdminAction(adminId, "BAN_USER_PERMANENT", userId, "USER", 
                              String.format("永久封禁用户，原因: %s", reason));
                
                log.info("用户 {} 已被永久封禁", userId);
                break;
                
            case "remove_member":
                // 移除群成员，需要额外的群组ID信息
                // 通常这个操作是在处理GROUP_MEMBER类型的举报时使用
                log.info("操作需要额外信息: 移除群组成员 {} 需要提供群组ID", userId);
                // 这里可能需要从report.reportedContentId获取群组ID
                break;
                
            case "ban_user":
                // 封禁用户，类似于temporary_ban或permanent_ban
                log.info("封禁用户 {}，原因: {}", userId, reason);
                // TODO: 调用用户服务的封禁方法
                break;
                
            default:
                log.warn("未知的用户操作类型: {}", action);
        }
    }

    /**
     * 处理对内容的操作
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param action 操作
     * @param banDuration 封禁时长（小时），用于群组封禁
     * @param reason 原因
     * @param adminId 管理员ID
     */
    private void processContentAction(String contentType, Long contentId, String action, Integer banDuration, String reason, Long adminId) {
        switch (contentType) {
            case "MESSAGE":
                processMessageAction(contentId, action, reason, adminId);
                break;
                
            case "GROUP":
                processGroupAction(contentId, action, banDuration, reason, adminId);
                break;
                
            case "GROUP_MEMBER":
                // 对于GROUP_MEMBER类型的内容操作，应该去找对应的举报记录
                try {
                    // 查找相关举报记录，获取被举报用户ID
                    List<Report> reports = reportRepository.findByReportedContentTypeAndReportedContentId("GROUP_MEMBER", contentId);
                    if (!reports.isEmpty()) {
                        Report report = reports.get(0); // 取最近的一条
                        Long userId = report.getReportedUserId();
                        if (userId != null) {
                            processGroupMemberAction(contentId, userId, action, reason, adminId);
                        } else {
                            log.warn("GROUP_MEMBER类型举报缺少被举报用户ID: reportId={}, contentId={}", report.getId(), contentId);
                        }
                    } else {
                        log.warn("找不到GROUP_MEMBER类型的举报记录: contentId={}", contentId);
                    }
                } catch (Exception e) {
                    log.error("处理GROUP_MEMBER内容操作失败: {}", e.getMessage(), e);
                    throw new BusinessException("处理GROUP_MEMBER内容操作失败: " + e.getMessage());
                }
                break;
                
            case "MOMENT":
                processMomentAction(contentId, action, reason, adminId);
                break;
                
            case "USER":
                // 对于USER类型，通常不会有内容操作，因为已经在processUserAction中处理了
                log.warn("用户类型不应该有内容操作: {}", action);
                break;
                
            default:
                log.warn("未知的内容类型: {}", contentType);
        }
    }

    /**
     * 处理对消息的操作
     */
    private void processMessageAction(Long messageId, String action, String reason, Long adminId) {
        // 检查消息是否存在
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        
        if ("delete".equals(action)) {
            // 删除消息
            log.info("删除消息 {}, 原因: {}", messageId, reason);
            
            // 设置消息状态为已删除
            message.setStatus(MessageStatus.DELETED);
            message.setDeletedAt(LocalDateTime.now());
            messageRepository.save(message);
            
            // 记录管理员操作
            logAdminAction(adminId, "DELETE_MESSAGE", messageId, "消息", reason);
            
            // 通知消息删除事件
            notifyMessageDeleted(message);
        } else {
            log.warn("未知的消息操作类型: {}", action);
        }
    }

    /**
     * 处理对群组的操作
     */
    private void processGroupAction(Long groupId, String action, Integer banDuration, String reason, Long adminId) {
        // 检查群组是否存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
        
        switch (action) {
            case "warn":
                // 向群组发送警告
                log.info("向群组 {} 发送警告: {}", groupId, reason);
                // TODO: 实现发送警告的逻辑
                break;
                
            case "temporary_ban":
                // 临时封禁群组
                if (banDuration == null || banDuration <= 0) {
                    throw new BusinessException("封禁时长必须大于0");
                }
                
                log.info("临时封禁群组 {}，时长: {} 小时，原因: {}", groupId, banDuration, reason);
                // 计算封禁截止时间
                LocalDateTime bannedUntil = LocalDateTime.now().plusHours(banDuration);
                
                // 设置群组封禁状态
                group.setIsBanned(true);
                group.setBannedUntil(bannedUntil);
                group.setBannedReason(reason);
                groupRepository.save(group);
                
                // 记录管理员操作
                logAdminAction(adminId, "BAN_GROUP_TEMPORARY", groupId, "GROUP", 
                             String.format("临时封禁群组，时长: %d小时，原因: %s", banDuration, reason));
                
                log.info("群组 {} 已被临时封禁，截止时间: {}", groupId, bannedUntil);
                break;
                
            case "permanent_ban":
                // 永久封禁群组
                log.info("永久封禁群组 {}，原因: {}", groupId, reason);
                
                // 设置群组封禁状态（永久封禁不设置截止时间）
                group.setIsBanned(true);
                group.setBannedUntil(null);
                group.setBannedReason(reason);
                groupRepository.save(group);
                
                // 记录管理员操作
                logAdminAction(adminId, "BAN_GROUP_PERMANENT", groupId, "GROUP", 
                             String.format("永久封禁群组，原因: %s", reason));
                
                log.info("群组 {} 已被永久封禁", groupId);
                break;
                
            case "dissolve":
                // 解散群组
                log.info("解散群组 {}，原因: {}", groupId, reason);
                
                try {
                    // 1. 删除所有成员关系
                    groupMemberRepository.deleteAllByIdGroupId(groupId);
                    
                    // 2. 删除群组实体
                    groupRepository.delete(group);
                    
                    // 记录管理员操作
                    logAdminAction(adminId, "DISSOLVE_GROUP", groupId, "GROUP", 
                                 String.format("解散群组，原因: %s", reason));
                    
                    log.info("群组 {} 已被解散", groupId);
                } catch (Exception e) {
                    log.error("解散群组 {} 失败: {}", groupId, e.getMessage(), e);
                    throw new BusinessException("解散群组失败: " + e.getMessage());
                }
                break;
                
            case "ban_group":
                // 封禁群组，默认永久封禁
                log.info("封禁群组 {}，原因: {}", groupId, reason);
                
                // 设置群组封禁状态
                group.setIsBanned(true);
                group.setBannedUntil(null); // 永久封禁
                group.setBannedReason(reason);
                groupRepository.save(group);
                
                // 记录管理员操作
                logAdminAction(adminId, "BAN_GROUP", groupId, "GROUP", 
                             String.format("封禁群组，原因: %s", reason));
                
                log.info("群组 {} 已被封禁", groupId);
                break;
                
            default:
                log.warn("未知的群组操作类型: {}", action);
        }
    }

    /**
     * 处理对群成员的操作
     * 
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param action 操作类型
     * @param reason 原因
     * @param adminId 管理员ID
     */
    private void processGroupMemberAction(Long groupId, Long userId, String action, String reason, Long adminId) {
        try {
            // 检查用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 检查群组是否存在
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new BusinessException("群组不存在"));
            
            switch (action) {
                case "remove_member":
                    // 检查用户是否为群成员
                    Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId);
                    if (memberOpt.isEmpty()) {
                        log.warn("用户不是群组成员: userId={}, groupId={}", userId, groupId);
                        throw new BusinessException("用户不是此群组的成员");
                    }
                    
                    // 移除群成员
                    groupMemberRepository.deleteByIdGroupIdAndIdUserId(groupId, userId);
                    
                    // 记录管理员操作
                    logAdminAction(adminId, "REMOVE_GROUP_MEMBER", userId, "GROUP_MEMBER", 
                                 String.format("移除群组成员，群组ID: %d，原因: %s", groupId, reason));
                    
                    log.info("已将用户 {} 从群组 {} 中移除", userId, groupId);
                    break;
                
                case "ban_user":
                    // 封禁用户
                    user.setIsBanned(true);
                    user.setBannedReason(reason);
                    user.setBannedUntil(null); // 永久封禁
                    userRepository.save(user);
                    
                    // 同时移除该成员
                    groupMemberRepository.deleteByIdGroupIdAndIdUserId(groupId, userId);
                    
                    // 记录管理员操作
                    logAdminAction(adminId, "BAN_USER_FROM_GROUP", userId, "USER", 
                                 String.format("封禁用户并移除群组成员身份，群组ID: %d，原因: %s", groupId, reason));
                    
                    log.info("用户 {} 已被封禁并从群组 {} 中移除", userId, groupId);
                    break;
                    
                default:
                    log.warn("未知的群成员操作类型: {}", action);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("处理群组成员操作失败: {}", e.getMessage(), e);
            throw new BusinessException("处理群组成员操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理对动态的操作
     */
    private void processMomentAction(Long momentId, String action, String reason, Long adminId) {
        // 检查动态是否存在
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new BusinessException("动态不存在"));
        
        if ("delete".equals(action)) {
            // 删除动态
            log.info("删除动态 {}, 原因: {}", momentId, reason);
            
            // 直接删除动态
            momentRepository.delete(moment);
            
            // 记录管理员操作
            logAdminAction(adminId, "DELETE_MOMENT", momentId, "动态", reason);
        } else {
            log.warn("未知的动态操作类型: {}", action);
        }
    }
    
    /**
     * 记录管理员操作日志
     */
    private void logAdminAction(Long adminId, String actionType, Long targetId, String targetType, String reason) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setOperationTypeString(actionType);
            log.setTargetTypeString(targetType.toUpperCase());
            log.setTargetId(targetId);
            log.setDescription(String.format("管理员删除%s: ID=%d, 原因: %s", targetType, targetId, reason));
            log.setCreatedAt(LocalDateTime.now());
            adminOperationLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录管理员操作日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 通知消息被删除
     */
    private void notifyMessageDeleted(Message message) {
        if (webSocketService == null) {
            log.error("WebSocketService未注入，无法发送消息删除通知");
            return;
        }
        
        try {
            // 准备WebSocket消息
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("messageId", message.getId());
            notificationData.put("conversationId", message.getConversationId());
            notificationData.put("type", "MESSAGE_DELETED");
            notificationData.put("reason", "违规内容已被管理员删除");
            
            // 发送WebSocket通知
            webSocketService.sendMessageToConversation(message.getConversationId(), notificationData, null);
            log.info("消息删除通知发送成功: messageId={}, conversationId={}", message.getId(), message.getConversationId());
        } catch (Exception e) {
            log.error("发送消息删除通知失败: messageId={}, error={}", message.getId(), e.getMessage(), e);
        }
    }

    @Override
    public Object getReportStatistics() {
        log.info("获取举报统计信息");
        
        // 统计各状态的举报数量
        Map<String, Object> statistics = new HashMap<>();
        Map<String, Long> statusCount = new HashMap<>();
        
        for (Report.ReportStatus status : Report.ReportStatus.values()) {
            long count = reportRepository.countByStatus(status);
            statusCount.put(status.name(), count);
        }
        
        statistics.put("statusCount", statusCount);
        
        // 统计各内容类型的举报数量
        List<Object[]> contentTypeCounts = reportRepository.countByContentType();
        Map<String, Long> contentTypeCount = new HashMap<>();
        
        for (Object[] result : contentTypeCounts) {
            String contentType = (String) result[0];
            Long count = (Long) result[1];
            if (contentType != null) {
                contentTypeCount.put(contentType, count);
            }
        }
        
        statistics.put("contentTypeCount", contentTypeCount);
        
        return statistics;
    }

    @Override
    @Transactional
    public ReportResponse createReport(Long reporterId, Long reportedUserId, String reportedContentType, 
                                    Long reportedContentId, String reason, String description) {
        log.info("创建新举报，举报者ID：{}，被举报用户ID：{}，内容类型：{}，内容ID：{}，原因：{}",
                reporterId, reportedUserId, reportedContentType, reportedContentId, reason);
        
        // 验证参数
        if (reporterId == null || !StringUtils.hasText(reportedContentType) || reportedContentId == null || !StringUtils.hasText(reason)) {
            throw new BusinessException("请求参数无效");
        }
        
        // 创建举报实体
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reportedContentType(reportedContentType)
                .reportedContentId(reportedContentId)
                .reason(reason)
                .description(description)
                .status(Report.ReportStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 保存举报
        Report savedReport = reportRepository.save(report);
        
        // 获取用户信息
        Map<Long, User> userMap = new HashMap<>();
        
        Optional<User> reporterOpt = userRepository.findById(reporterId);
        reporterOpt.ifPresent(user -> userMap.put(user.getId(), user));
        
        if (reportedUserId != null) {
            Optional<User> reportedUserOpt = userRepository.findById(reportedUserId);
            reportedUserOpt.ifPresent(user -> userMap.put(user.getId(), user));
        }
        
        return convertToReportResponse(savedReport, userMap);
    }
    
    /**
     * 将举报实体转换为DTO
     *
     * @param report 举报实体
     * @param userMap 用户映射表
     * @return 举报响应DTO
     */
    private ReportResponse convertToReportResponse(Report report, Map<Long, User> userMap) {
        User reporter = userMap.get(report.getReporterId());
        User reportedUser = report.getReportedUserId() != null ? userMap.get(report.getReportedUserId()) : null;
        
        return ReportResponse.builder()
                .reportId(report.getId())
                .reporterId(report.getReporterId())
                .reporterUsername(reporter != null ? reporter.getNickname() : "未知用户")
                .reportedUserId(report.getReportedUserId())
                .reportedUsername(reportedUser != null ? reportedUser.getNickname() : null)
                .reportedContentType(report.getReportedContentType())
                .reportedContentId(report.getReportedContentId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .handledAt(report.getHandledAt())
                .handledBy(report.getHandledBy())
                .build();
    }
} 