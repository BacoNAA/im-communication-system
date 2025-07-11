package com.im.imcommunicationsystem.user.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.imcommunicationsystem.auth.entity.User;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 用户状态清理定时任务
 * 负责清理过期的用户状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusCleanupTask {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 每30秒执行一次状态过期检查
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanupExpiredStatuses() {
        log.debug("开始执行用户状态过期清理任务");
        
        try {
            // 获取所有有状态的用户
            List<User> usersWithStatus = userRepository.findByStatusJsonIsNotNull();
            
            int cleanedCount = 0;
            LocalDateTime now = LocalDateTime.now();
            
            for (User user : usersWithStatus) {
                if (isStatusExpired(user, now)) {
                    // 清除过期状态
                    user.setStatusJson(null);
                    userRepository.save(user);
                    cleanedCount++;
                    log.info("清除用户过期状态，用户ID: {}", user.getId());
                }
            }
            
            if (cleanedCount > 0) {
                log.info("状态过期清理任务完成，清理了 {} 个过期状态", cleanedCount);
            } else {
                log.debug("状态过期清理任务完成，没有发现过期状态");
            }
            
        } catch (Exception e) {
            log.error("执行状态过期清理任务时发生错误", e);
        }
    }

    /**
     * 检查用户状态是否过期
     * 
     * @param user 用户对象
     * @param now 当前时间
     * @return 是否过期
     */
    private boolean isStatusExpired(User user, LocalDateTime now) {
        String statusJson = user.getStatusJson();
        if (statusJson == null || statusJson.trim().isEmpty()) {
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> statusMap = objectMapper.readValue(statusJson, Map.class);
            
            Object expiresAtObj = statusMap.get("expiresAt");
            if (expiresAtObj == null) {
                // 没有过期时间，状态永不过期
                return false;
            }
            
            String expiresAtStr = expiresAtObj.toString();
            LocalDateTime expiresAt;
            
            // 尝试不同的时间格式解析
            try {
                // 首先尝试ISO格式（前端发送的格式）
                if (expiresAtStr.contains("T") && expiresAtStr.endsWith("Z")) {
                    // ISO格式带Z后缀，需要去掉Z并转换为LocalDateTime
                    expiresAt = LocalDateTime.parse(expiresAtStr.substring(0, expiresAtStr.length() - 1));
                } else if (expiresAtStr.contains("T")) {
                    // ISO格式不带Z后缀
                    expiresAt = LocalDateTime.parse(expiresAtStr);
                } else {
                    // LocalDateTime.toString()格式
                    expiresAt = LocalDateTime.parse(expiresAtStr);
                }
            } catch (Exception parseException) {
                log.warn("无法解析过期时间格式: {}, 用户ID: {}", expiresAtStr, user.getId(), parseException);
                return false;
            }
            
            boolean expired = now.isAfter(expiresAt);
            if (expired) {
                log.debug("用户状态已过期，用户ID: {}, 过期时间: {}, 当前时间: {}", 
                    user.getId(), expiresAt, now);
            }
            
            return expired;
            
        } catch (JsonProcessingException e) {
            log.warn("解析用户状态JSON失败，用户ID: {}, 将视为未过期", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.warn("检查用户状态过期时发生错误，用户ID: {}, 将视为未过期", user.getId(), e);
            return false;
        }
    }
}