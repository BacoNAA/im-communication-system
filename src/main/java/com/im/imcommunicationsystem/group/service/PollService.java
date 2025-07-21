package com.im.imcommunicationsystem.group.service;

import com.im.imcommunicationsystem.group.dto.request.CreatePollRequest;
import com.im.imcommunicationsystem.group.dto.request.VotePollRequest;
import com.im.imcommunicationsystem.group.dto.response.PollResponse;
import com.im.imcommunicationsystem.group.enums.PollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 群投票服务接口
 */
public interface PollService {
    
    /**
     * 创建投票
     *
     * @param groupId 群组ID
     * @param userId 创建者ID
     * @param request 创建投票请求
     * @return 创建的投票信息
     */
    PollResponse createPoll(Long groupId, Long userId, CreatePollRequest request);
    
    /**
     * 获取投票详情
     *
     * @param pollId 投票ID
     * @param userId 当前用户ID
     * @return 投票详情
     */
    PollResponse getPollById(Long pollId, Long userId);
    
    /**
     * 分页获取群组的投票列表
     *
     * @param groupId 群组ID
     * @param status 投票状态（可选）
     * @param userId 当前用户ID
     * @param pageable 分页参数
     * @return 投票列表
     */
    Page<PollResponse> getGroupPolls(Long groupId, PollStatus status, Long userId, Pageable pageable);
    
    /**
     * 参与投票
     *
     * @param pollId 投票ID
     * @param userId 投票者ID
     * @param request 投票请求
     * @return 更新后的投票信息
     */
    PollResponse voteOnPoll(Long pollId, Long userId, VotePollRequest request);
    
    /**
     * 结束投票
     *
     * @param pollId 投票ID
     * @param groupId 群组ID
     * @param userId 操作者ID
     * @return 更新后的投票信息
     */
    PollResponse endPoll(Long pollId, Long groupId, Long userId);
    
    /**
     * 取消投票
     *
     * @param pollId 投票ID
     * @param groupId 群组ID
     * @param userId 操作者ID
     * @return 更新后的投票信息
     */
    PollResponse cancelPoll(Long pollId, Long groupId, Long userId);
} 