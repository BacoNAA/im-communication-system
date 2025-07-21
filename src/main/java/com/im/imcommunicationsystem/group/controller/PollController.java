package com.im.imcommunicationsystem.group.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.common.exception.BusinessException;
import com.im.imcommunicationsystem.group.dto.request.CreatePollRequest;
import com.im.imcommunicationsystem.group.dto.request.VotePollRequest;
import com.im.imcommunicationsystem.group.dto.response.PollResponse;
import com.im.imcommunicationsystem.group.enums.PollStatus;
import com.im.imcommunicationsystem.group.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;

/**
 * 群投票控制器
 */
@RestController
@RequestMapping("/api/groups/{groupId}/polls")
@RequiredArgsConstructor
@Slf4j
public class PollController {
    
    private final PollService pollService;
    
    /**
     * 创建投票
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PollResponse>> createPoll(
            @PathVariable Long groupId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePollRequest request
    ) {
        try {
            log.info("创建投票: groupId={}, userId={}, title={}", groupId, userId, request.getTitle());
            PollResponse response = pollService.createPoll(groupId, userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("创建投票业务异常: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("创建投票系统异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "创建投票失败，请稍后重试"));
        }
    }
    
    /**
     * 获取群组投票列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PollResponse>>> getGroupPolls(
            @PathVariable Long groupId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            log.info("获取群组投票列表: groupId={}, userId={}, status={}, page={}, size={}", 
                    groupId, userId, status, page, size);
            
            // 处理状态枚举
            PollStatus pollStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    pollStatus = PollStatus.valueOf(status.toUpperCase());
                    log.info("转换状态枚举成功: {}", pollStatus);
                } catch (IllegalArgumentException e) {
                    log.warn("无效的状态值: {}, 有效值: {}", status, Arrays.toString(PollStatus.values()));
                }
            }
            
            // 记录PollStatus枚举值的所有可能值
            log.info("可用的PollStatus枚举值: {}", Arrays.toString(PollStatus.values()));
            
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PollResponse> response = pollService.getGroupPolls(groupId, pollStatus, userId, pageRequest);
            
            log.info("获取群组投票列表成功: groupId={}, userId={}, status={}, totalElements={}", 
                    groupId, userId, status, response.getTotalElements());
                    
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("获取群组投票列表业务异常: groupId={}, userId={}, status={}, error={}", 
                    groupId, userId, status, e.getMessage());
            // 返回空列表而非错误，避免前端显示错误
            Page<PollResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
            return ResponseEntity.ok(ApiResponse.success(emptyPage));
        } catch (Exception e) {
            log.error("获取群组投票列表系统异常: groupId={}, userId={}, status={}, error={}", 
                    groupId, userId, status, e.getMessage(), e);
            // 返回空列表而非错误，避免前端显示错误
            Page<PollResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
            return ResponseEntity.ok(ApiResponse.success(emptyPage));
        }
    }
    
    /**
     * 获取投票详情
     */
    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollResponse>> getPollDetails(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        try {
            log.info("获取投票详情: groupId={}, pollId={}, userId={}", groupId, pollId, userId);
            PollResponse response = pollService.getPollById(pollId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("获取投票详情业务异常: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("获取投票详情系统异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "获取投票详情失败，请稍后重试"));
        }
    }
    
    /**
     * 参与投票
     */
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<ApiResponse<PollResponse>> voteOnPoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VotePollRequest request
    ) {
        try {
            log.info("参与投票: groupId={}, pollId={}, userId={}, optionIds={}", 
                    groupId, pollId, userId, request.getOptionIds());
            
            // 验证请求参数
            if (request == null || request.getOptionIds() == null || request.getOptionIds().isEmpty()) {
                log.warn("投票请求缺少必要参数: request={}", request);
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "投票选项不能为空"));
            }
            
            PollResponse response = pollService.voteOnPoll(pollId, userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("参与投票业务异常: groupId={}, pollId={}, userId={}, error={}", 
                    groupId, pollId, userId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("参与投票系统异常: groupId={}, pollId={}, userId={}, error={}", 
                    groupId, pollId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "参与投票失败，请稍后重试"));
        }
    }
    
    /**
     * 结束投票
     */
    @PostMapping("/{pollId}/end")
    public ResponseEntity<ApiResponse<PollResponse>> endPoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        try {
            log.info("结束投票: groupId={}, pollId={}, userId={}", groupId, pollId, userId);
            PollResponse response = pollService.endPoll(pollId, groupId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("结束投票业务异常: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("结束投票系统异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "结束投票失败，请稍后重试"));
        }
    }
    
    /**
     * 取消投票
     */
    @PostMapping("/{pollId}/cancel")
    public ResponseEntity<ApiResponse<PollResponse>> cancelPoll(
            @PathVariable Long groupId,
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        try {
            log.info("取消投票: groupId={}, pollId={}, userId={}", groupId, pollId, userId);
            PollResponse response = pollService.cancelPoll(pollId, groupId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("取消投票业务异常: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("取消投票系统异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "取消投票失败，请稍后重试"));
        }
    }
} 