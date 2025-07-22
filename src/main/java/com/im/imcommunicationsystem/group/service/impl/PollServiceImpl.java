package com.im.imcommunicationsystem.group.service.impl;

import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.group.dto.request.CreatePollRequest;
import com.im.imcommunicationsystem.group.dto.request.VotePollRequest;
import com.im.imcommunicationsystem.group.dto.response.PollOptionResponse;
import com.im.imcommunicationsystem.group.dto.response.PollResponse;
import com.im.imcommunicationsystem.group.dto.response.VoterResponse;
import com.im.imcommunicationsystem.group.entity.Group;
import com.im.imcommunicationsystem.group.entity.GroupMember;
import com.im.imcommunicationsystem.group.entity.Poll;
import com.im.imcommunicationsystem.group.entity.PollOption;
import com.im.imcommunicationsystem.group.entity.PollVote;
import com.im.imcommunicationsystem.group.enums.GroupMemberRole;
import com.im.imcommunicationsystem.group.enums.PollStatus;
import com.im.imcommunicationsystem.group.repository.GroupMemberRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.group.repository.PollOptionRepository;
import com.im.imcommunicationsystem.group.repository.PollRepository;
import com.im.imcommunicationsystem.group.repository.PollVoteRepository;
import com.im.imcommunicationsystem.group.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 群投票服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    @Transactional
    public PollResponse createPoll(Long groupId, Long userId, CreatePollRequest request) {
        log.info("创建群投票: groupId={}, userId={}, title={}", groupId, userId, request.getTitle());
        
        // 检查群组是否存在
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
        
        // 检查群组是否被封禁
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new BusinessException("该群组已被封禁，无法创建投票" + reason);
        }
        
        // 验证用户是否为群组成员
        if (!groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId).isPresent()) {
            throw new BusinessException("您不是该群组成员，无法创建投票");
        }
        
        // 验证请求参数
        if (request == null) {
            throw new BusinessException("投票信息不能为空");
        }
        if (StringUtils.isBlank(request.getTitle())) {
            throw new BusinessException("投票标题不能为空");
        }
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new BusinessException("至少需要一个投票选项");
        }
        
        // 详细记录结束时间验证
        if (request.getEndTime() != null) {
            LocalDateTime currentTime = LocalDateTime.now();
            log.info("验证结束时间: 当前时间={}, 请求结束时间={}, 差值={}秒", 
                currentTime, 
                request.getEndTime(), 
                ChronoUnit.SECONDS.between(currentTime, request.getEndTime()));
                
            // 允许结束时间在当前时间前最多30秒的误差，以处理时区和网络延迟问题
            if (request.getEndTime().isBefore(currentTime.minusSeconds(30))) {
                log.warn("结束时间早于当前时间30秒以上: 当前时间={}, 结束时间={}", 
                    currentTime, request.getEndTime());
                throw new BusinessException("结束时间必须晚于当前时间");
            }
        }
        
        // 创建投票
        Poll poll = Poll.builder()
                .groupId(groupId)
                .creatorId(userId)
                .title(request.getTitle())
                .description(StringUtils.isBlank(request.getDescription()) ? null : request.getDescription())
                .isMultiple(request.getIsMultiple() != null && request.getIsMultiple())
                .isAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous())
                .endTime(request.getEndTime())
                .status(PollStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 处理结束时间
        if (request.getEndTime() != null) {
            // 记录处理前的结束时间，用于调试
            log.debug("原始结束时间: {}", request.getEndTime());
            
            // 获取系统时区信息
            ZoneId systemZone = ZoneId.systemDefault();
            log.info("系统时区: {}, 当前时间: {}", systemZone, LocalDateTime.now());
            
            LocalDateTime adjustedTime = request.getEndTime();
            
            // 如果前端发送的是UTC时间，可以显式转换为系统默认时区
            if (request.getEndTime().toString().endsWith("Z")) {
                log.info("检测到UTC时间格式，进行时区转换");
                adjustedTime = request.getEndTime().atZone(ZoneOffset.UTC).withZoneSameInstant(systemZone).toLocalDateTime();
                log.info("转换后的结束时间: {}", adjustedTime);
            }
            
            // 确保调整后的时间也晚于当前时间，但允许30秒的误差
            LocalDateTime currentTime = LocalDateTime.now();
            if (adjustedTime.isBefore(currentTime.minusSeconds(30))) {
                log.warn("调整后的时间仍早于当前时间: 当前时间={}, 调整后时间={}", 
                    currentTime, adjustedTime);
                throw new BusinessException("结束时间必须晚于当前时间");
            }
            
            // 设置调整后的时间
            poll.setEndTime(adjustedTime);
            
            log.debug("最终设置的结束时间: {}", poll.getEndTime());
        }
        
        poll = pollRepository.save(poll);
        
        // 创建投票选项
        List<PollOption> options = new ArrayList<>();
        int order = 0;
        for (String optionText : request.getOptions()) {
            if (StringUtils.isBlank(optionText)) {
                continue; // 忽略空选项
            }
            
            PollOption option = PollOption.builder()
                    .poll(poll)
                    .pollId(poll.getId())
                    .optionText(optionText)
                    .displayOrder(order++)
                    .build();
            options.add(option);
        }
        
        options = pollOptionRepository.saveAll(options);
        poll.setOptions(options);
        
        // 构建响应
        return buildPollResponse(poll, userId, new HashMap<>());
    }
    
    /**
     * 检查和更新过期投票状态
     * @param poll 投票对象
     * @return 如果状态被更新返回true，否则返回false
     */
    private boolean checkAndUpdateExpiredPollStatus(Poll poll) {
        if (poll.getStatus() == PollStatus.ACTIVE && 
            poll.getEndTime() != null && 
            poll.getEndTime().isBefore(LocalDateTime.now())) {
            
            log.info("投票已过期，更新状态: pollId={}, endTime={}", poll.getId(), poll.getEndTime());
            poll.setStatus(PollStatus.ENDED);
            pollRepository.save(poll);
            return true;
        }
        return false;
    }
    
    /**
     * 批量检查和更新过期投票状态
     * @param polls 投票对象列表
     * @return 更新的投票数量
     */
    private int batchCheckAndUpdateExpiredPollStatus(List<Poll> polls) {
        int updatedCount = 0;
        for (Poll poll : polls) {
            if (checkAndUpdateExpiredPollStatus(poll)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
    
    @Override
    public PollResponse getPollById(Long pollId, Long userId) {
        log.info("获取投票详情: pollId={}, userId={}", pollId, userId);
        
        // 获取投票信息
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException("投票不存在"));
        
        // 检查是否过期
        checkAndUpdateExpiredPollStatus(poll);
        
        // 验证用户是否为群组成员
        if (!groupMemberRepository.findByIdGroupIdAndIdUserId(poll.getGroupId(), userId).isPresent()) {
            throw new BusinessException("您不是该群组成员，无法查看投票");
        }
        
        // 获取投票选项和投票数据
        Map<Long, Long> optionVoteCount = getOptionVoteCount(pollId);
        
        // 构建响应
        return buildPollResponse(poll, userId, optionVoteCount);
    }
    
    @Override
    public Page<PollResponse> getGroupPolls(Long groupId, PollStatus status, Long userId, Pageable pageable) {
        try {
            log.info("获取群组投票列表: groupId={}, userId={}, status={}", groupId, userId, status);
            
            // 验证群组是否存在
            if (!groupRepository.existsById(groupId)) {
                throw new BusinessException("群组不存在");
            }
            
            // 验证用户是否为群组成员
            if (!groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId).isPresent()) {
                throw new BusinessException("您不是该群组成员，无法查看投票");
            }
            
            // 获取投票列表
            Page<Poll> pollsPage;
            try {
                if (status != null) {
                    log.debug("按状态查询: status={}", status);
                    pollsPage = pollRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, status, pageable);
                } else {
                    log.debug("查询所有状态");
                    pollsPage = pollRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
                }
            } catch (Exception e) {
                log.error("查询投票数据出错: groupId={}, status={}, error={}", groupId, status, e.getMessage(), e);
                // 返回空结果页
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            
            log.debug("查询到投票数量: {}", pollsPage.getTotalElements());
            
            // 检查并更新过期投票状态
            List<Poll> polls = pollsPage.getContent();
            int updatedCount = batchCheckAndUpdateExpiredPollStatus(polls);
            if (updatedCount > 0) {
                log.info("已将{}个过期投票状态更新为已结束", updatedCount);
                
                // 如果是按ACTIVE状态筛选，且有投票状态被更新，则需要重新查询
                if (status == PollStatus.ACTIVE && updatedCount > 0) {
                    log.debug("重新查询活动状态投票");
                    pollsPage = pollRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, status, pageable);
                    polls = pollsPage.getContent();
                }
            }
            
            // 获取所有投票的选项和投票数据
            List<Long> pollIds = polls.stream()
                    .map(Poll::getId)
                    .collect(Collectors.toList());
            
            Map<Long, Map<Long, Long>> allOptionVoteCounts = new HashMap<>();
            
            if (!pollIds.isEmpty()) {
                try {
                    // 一次性查询所有投票的票数统计
                    List<Object[]> voteStats = pollVoteRepository.countVotesByPollIdsGroupByPollAndOption(pollIds);
                    
                    if (voteStats != null) {
                        for (Object[] stat : voteStats) {
                            if (stat != null && stat.length >= 3 && stat[0] != null && stat[1] != null && stat[2] != null) {
                                try {
                                    Long pollId = ((Number) stat[0]).longValue();
                                    Long optionId = ((Number) stat[1]).longValue();
                                    Long count = ((Number) stat[2]).longValue();
                                    
                                    allOptionVoteCounts
                                        .computeIfAbsent(pollId, k -> new HashMap<>())
                                        .put(optionId, count);
                                } catch (ClassCastException e) {
                                    log.error("统计数据类型转换错误: {}", e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("获取投票统计信息时出错: {}", e.getMessage(), e);
                    // 即使获取票数统计失败，也继续构建响应，只是票数为0
                }
            }
            
            // 构建响应
            List<PollResponse> responses = new ArrayList<>();
            
            for (Poll poll : pollsPage.getContent()) {
                try {
                    Map<Long, Long> optionVoteCount = allOptionVoteCounts.getOrDefault(poll.getId(), new HashMap<>());
                    PollResponse response = buildPollResponse(poll, userId, optionVoteCount);
                    responses.add(response);
                } catch (Exception e) {
                    log.error("构建投票响应对象时出错，投票ID={}: {}", poll.getId(), e.getMessage(), e);
                    // 跳过处理有错误的投票
                }
            }
            
            return new PageImpl<>(responses, pageable, pollsPage.getTotalElements());
        } catch (Exception e) {
            log.error("获取群组投票列表时出错: groupId={}, status={}, userId={}, error={}", 
                    groupId, status, userId, e.getMessage(), e);
            throw new BusinessException("获取投票列表失败，请稍后重试");
        }
    }
    
    /**
     * 参与投票
     */
    @Override
    @Transactional
    public PollResponse voteOnPoll(Long pollId, Long userId, VotePollRequest request) {
        try {
            log.info("参与投票: pollId={}, userId={}, options={}", pollId, userId, request.getOptionIds());
            
            if (request == null || request.getOptionIds() == null || request.getOptionIds().isEmpty()) {
                throw new BusinessException("投票选项不能为空");
            }
            
            // 获取投票信息
            Poll poll;
            try {
                poll = pollRepository.findById(pollId)
                    .orElseThrow(() -> new BusinessException("投票不存在"));
                log.debug("找到投票: {}", poll.getTitle());
            } catch (Exception e) {
                log.error("查询投票信息失败: pollId={}, error={}", pollId, e.getMessage(), e);
                throw new BusinessException("投票不存在或已被删除");
            }
            
            // 检查群组是否被封禁
            Group group = groupRepository.findById(poll.getGroupId())
                    .orElseThrow(() -> new BusinessException("群组不存在"));
            if (Boolean.TRUE.equals(group.getIsBanned())) {
                String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
                throw new BusinessException("该群组已被封禁，无法参与投票" + reason);
            }
            
            // 检查是否过期，如果过期则自动更新状态
            if (checkAndUpdateExpiredPollStatus(poll)) {
                throw new BusinessException("该投票已过期，无法参与");
            }
            
            // 验证投票是否处于活动状态
            if (poll.getStatus() != PollStatus.ACTIVE) {
                log.warn("尝试参与非活动状态的投票: pollId={}, userId={}, status={}", 
                        pollId, userId, poll.getStatus());
                throw new BusinessException("该投票已结束或已取消，无法参与");
            }
            
            // 验证结束时间
            if (poll.getEndTime() != null && poll.getEndTime().isBefore(LocalDateTime.now())) {
                log.info("投票已过期，自动更新状态: pollId={}, endTime={}", pollId, poll.getEndTime());
                poll.setStatus(PollStatus.ENDED);
                pollRepository.save(poll);
                throw new BusinessException("该投票已过期，无法参与");
            }
            
            // 验证用户是否为群组成员
            try {
                boolean isMember = groupMemberRepository.findByIdGroupIdAndIdUserId(poll.getGroupId(), userId).isPresent();
                if (!isMember) {
                    log.warn("非群组成员尝试参与投票: pollId={}, userId={}, groupId={}", 
                            pollId, userId, poll.getGroupId());
                    throw new BusinessException("您不是该群组成员，无法参与投票");
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) throw e;
                log.error("验证群组成员资格失败: pollId={}, userId={}, error={}", 
                        pollId, userId, e.getMessage(), e);
                throw new BusinessException("验证用户资格失败，请稍后重试");
            }
            
            // 验证用户是否已经投过票
            try {
                boolean hasVoted = pollVoteRepository.existsByPollIdAndUserId(pollId, userId);
                if (hasVoted) {
                    log.warn("用户尝试重复投票: pollId={}, userId={}", pollId, userId);
                    throw new BusinessException("您已经参与过此投票，不能重复投票");
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) throw e;
                log.error("检查用户投票状态失败: pollId={}, userId={}, error={}", 
                        pollId, userId, e.getMessage(), e);
                throw new BusinessException("检查投票状态失败，请稍后重试");
            }
            
            // 验证是否为单选
            if (!poll.getIsMultiple() && request.getOptionIds().size() > 1) {
                log.warn("单选投票提交了多个选项: pollId={}, userId={}, optionCount={}", 
                        pollId, userId, request.getOptionIds().size());
                throw new BusinessException("此投票为单选，只能选择一个选项");
            }
            
            // 获取所有选项
            List<PollOption> options;
            try {
                options = pollOptionRepository.findAllById(request.getOptionIds());
                if (options.size() != request.getOptionIds().size()) {
                    log.warn("提交了无效的选项ID: pollId={}, requestedIds={}, foundIds={}", 
                            pollId, request.getOptionIds(), options.stream().map(PollOption::getId).collect(Collectors.toList()));
                    throw new BusinessException("部分选项无效");
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) throw e;
                log.error("查询投票选项失败: pollId={}, optionIds={}, error={}", 
                        pollId, request.getOptionIds(), e.getMessage(), e);
                throw new BusinessException("投票选项查询失败，请稍后重试");
            }
            
            for (PollOption option : options) {
                if (!option.getPollId().equals(pollId)) {
                    log.warn("选项不属于当前投票: pollId={}, optionId={}, belongsToPollId={}", 
                            pollId, option.getId(), option.getPollId());
                    throw new BusinessException("选项不属于当前投票");
                }
            }
            
            // 保存投票记录 - 修复关联对象设置
            List<PollVote> votes = new ArrayList<>();
            for (PollOption option : options) {
                PollVote vote = PollVote.builder()
                        .poll(poll)       // 设置关联的Poll对象而不是pollId
                        .option(option)   // 设置关联的PollOption对象而不是optionId
                        .userId(userId)
                        .votedAt(LocalDateTime.now())
                        .build();
                votes.add(vote);
            }
            
            try {
                pollVoteRepository.saveAll(votes);
                log.info("用户投票成功: pollId={}, userId={}, optionIds={}", 
                        pollId, userId, request.getOptionIds());
            } catch (Exception e) {
                log.error("保存投票记录失败: pollId={}, userId={}, error={}", 
                        pollId, userId, e.getMessage(), e);
                throw new BusinessException("保存投票失败，请稍后重试");
            }
            
            // 更新投票信息
            Map<Long, Long> optionVoteCount;
            try {
                optionVoteCount = getOptionVoteCount(pollId);
            } catch (Exception e) {
                log.error("获取投票统计失败: pollId={}, error={}", pollId, e.getMessage(), e);
                // 使用空映射继续处理
                optionVoteCount = new HashMap<>();
            }
            
            return buildPollResponse(poll, userId, optionVoteCount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("参与投票时发生意外错误: pollId={}, userId={}, error={}", 
                    pollId, userId, e.getMessage(), e);
            throw new BusinessException("参与投票失败，请稍后重试");
        }
    }
    
    @Override
    @Transactional
    public PollResponse endPoll(Long pollId, Long groupId, Long userId) {
        log.info("结束投票: pollId={}, groupId={}, userId={}", pollId, groupId, userId);
        
        // 获取投票信息
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException("投票不存在"));
        
        // 验证群组ID
        if (!poll.getGroupId().equals(groupId)) {
            throw new BusinessException("投票不属于该群组");
        }
        
        // 验证用户权限
        if (!isCreatorOrAdmin(poll.getGroupId(), userId, poll.getCreatorId())) {
            throw new BusinessException("您没有权限结束此投票");
        }
        
        // 检查群组是否被封禁
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("群组不存在"));
        if (Boolean.TRUE.equals(group.getIsBanned())) {
            String reason = group.getBannedReason() != null ? "，原因：" + group.getBannedReason() : "";
            throw new BusinessException("该群组已被封禁，无法结束投票" + reason);
        }
        
        // 更新投票状态
        poll.setStatus(PollStatus.ENDED);
        poll = pollRepository.save(poll);
        
        // 更新投票信息
        Map<Long, Long> optionVoteCount = getOptionVoteCount(pollId);
        return buildPollResponse(poll, userId, optionVoteCount);
    }
    
    @Override
    @Transactional
    public PollResponse cancelPoll(Long pollId, Long groupId, Long userId) {
        log.info("取消投票: pollId={}, groupId={}, userId={}", pollId, groupId, userId);
        
        // 获取投票信息
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException("投票不存在"));
        
        // 验证群组ID
        if (!poll.getGroupId().equals(groupId)) {
            throw new BusinessException("投票不属于该群组");
        }
        
        // 验证用户权限
        if (!isCreatorOrAdmin(poll.getGroupId(), userId, poll.getCreatorId())) {
            throw new BusinessException("您没有权限取消此投票");
        }
        
        // 更新投票状态
        poll.setStatus(PollStatus.CANCELED);
        poll = pollRepository.save(poll);
        
        // 更新投票信息
        Map<Long, Long> optionVoteCount = getOptionVoteCount(pollId);
        return buildPollResponse(poll, userId, optionVoteCount);
    }
    
    /**
     * 获取投票选项的投票数据
     */
    private Map<Long, Long> getOptionVoteCount(Long pollId) {
        List<Object[]> voteStats = pollVoteRepository.countVotesByPollIdGroupByOptionId(pollId);
        Map<Long, Long> optionVoteCount = new HashMap<>();
        
        for (Object[] stat : voteStats) {
            Long optionId = ((Number) stat[0]).longValue();
            Long count = ((Number) stat[1]).longValue();
            optionVoteCount.put(optionId, count);
        }
        
        return optionVoteCount;
    }
    
    /**
     * 检查用户是否为创建者或管理员
     */
    private boolean isCreatorOrAdmin(Long groupId, Long userId, Long creatorId) {
        if (userId.equals(creatorId)) {
            return true;
        }
        
        Optional<GroupMember> memberOpt = groupMemberRepository.findByIdGroupIdAndIdUserId(groupId, userId);
        if (memberOpt.isPresent()) {
            GroupMember member = memberOpt.get();
            return member.getRole() == GroupMemberRole.admin || member.getRole() == GroupMemberRole.owner;
        }
        
        return false;
    }
    
    /**
     * 构建投票响应对象
     */
    private PollResponse buildPollResponse(Poll poll, Long currentUserId, Map<Long, Long> optionVoteCount) {
        try {
            // 获取创建者信息
            User creator = null;
            String creatorNickname = "未知用户";
            String creatorAvatarUrl = "";
            
            try {
                creator = userRepository.findById(poll.getCreatorId()).orElse(null);
                if (creator != null) {
                    creatorNickname = creator.getNickname() != null ? creator.getNickname() : "未知用户";
                    creatorAvatarUrl = creator.getAvatarUrl() != null ? creator.getAvatarUrl() : "";
                }
            } catch (Exception e) {
                log.error("获取创建者信息时出错: {}", e.getMessage(), e);
                // 继续处理，使用默认值
            }
            
            // 获取投票选项列表
            List<PollOption> options;
            try {
                options = pollOptionRepository.findByPollIdOrderByDisplayOrder(poll.getId());
            } catch (Exception e) {
                log.error("获取投票选项时出错: {}", e.getMessage(), e);
                options = Collections.emptyList();
            }
            
            // 获取用户投票记录
            List<PollVote> userVotes;
            List<Long> userVotedOptionIds = new ArrayList<>();
            try {
                userVotes = pollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUserId);
                userVotedOptionIds = userVotes.stream()
                        .map(PollVote::getOptionId)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取用户投票记录时出错: {}", e.getMessage(), e);
                userVotes = Collections.emptyList();
            }
            
            // 计算总投票人数和选项百分比
            long totalVotes = optionVoteCount != null ? 
                    optionVoteCount.values().stream().mapToLong(Long::longValue).sum() : 0;
            
            // 构建选项响应列表
            List<PollOptionResponse> optionResponses = new ArrayList<>();
            
            for (PollOption option : options) {
                try {
                    long voteCount = optionVoteCount != null ? 
                            optionVoteCount.getOrDefault(option.getId(), 0L) : 0L;
                    double percentage = totalVotes > 0 ? (voteCount * 100.0 / totalVotes) : 0.0;
                    boolean isSelected = userVotedOptionIds.contains(option.getId());
                    
                    // 如果是匿名投票，不返回投票者信息
                    List<VoterResponse> voters = new ArrayList<>();
                    if (poll.getIsAnonymous() != null && !poll.getIsAnonymous()) {
                        // 在实际应用中，这里需要查询投票用户信息
                        // 由于简化起见，这里只返回空列表
                    }
                    
                    PollOptionResponse optionResponse = PollOptionResponse.builder()
                            .id(option.getId())
                            .optionText(option.getOptionText() != null ? option.getOptionText() : "")
                            .voteCount(voteCount)
                            .percentage(percentage)
                            .isSelected(isSelected)
                            .voters(voters)
                            .build();
                    
                    optionResponses.add(optionResponse);
                } catch (Exception e) {
                    log.error("构建选项响应对象时出错, 选项ID={}: {}", option.getId(), e.getMessage(), e);
                    // 跳过有错误的选项
                }
            }
            
            // 判断当前用户是否已投票
            boolean hasVoted = !userVotes.isEmpty();
            
            // 构建投票响应对象
            return PollResponse.builder()
                    .id(poll.getId())
                    .groupId(poll.getGroupId())
                    .creatorId(poll.getCreatorId())
                    .creatorNickname(creatorNickname)
                    .creatorAvatarUrl(creatorAvatarUrl)
                    .title(poll.getTitle() != null ? poll.getTitle() : "")
                    .description(poll.getDescription() != null ? poll.getDescription() : "")
                    .isMultiple(poll.getIsMultiple() != null ? poll.getIsMultiple() : false)
                    .isAnonymous(poll.getIsAnonymous() != null ? poll.getIsAnonymous() : false)
                    .status(poll.getStatus())
                    .createdAt(poll.getCreatedAt())
                    .endTime(poll.getEndTime())
                    .totalVoters(totalVotes)
                    .hasVoted(hasVoted)
                    .options(optionResponses)
                    .build();
        } catch (Exception e) {
            log.error("构建投票响应对象时出错: pollId={}, error={}", 
                    poll != null ? poll.getId() : "未知", e.getMessage(), e);
            throw new BusinessException("构建投票信息失败");
        }
    }
} 