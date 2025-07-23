package com.im.imcommunicationsystem.admin.service.impl;

import com.im.imcommunicationsystem.admin.dto.response.StatisticsResponse;
import com.im.imcommunicationsystem.admin.dto.response.SystemOverviewResponse;
import com.im.imcommunicationsystem.admin.service.AdminDashboardService;
import com.im.imcommunicationsystem.auth.repository.UserRepository;
import com.im.imcommunicationsystem.group.repository.GroupRepository;
import com.im.imcommunicationsystem.message.repository.MessageRepository;
import com.im.imcommunicationsystem.moment.repository.MomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员仪表盘服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final MomentRepository momentRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 获取系统概览数据
     * @param period 时间周期 (today, week, month, year)
     * @return 系统概览数据
     */
    @Override
    public SystemOverviewResponse getSystemOverview(String period) {
        log.info("获取系统概览数据, period={}", period);
        
        // 获取统计周期的起止时间
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 获取对比周期的起止时间（用于计算同比增长率）
        LocalDateTime compareStartTime = getStartTimeForComparePeriod(period);
        LocalDateTime compareEndTime = startTime;
        
        // 获取用户统计数据
        long totalUsers = userRepository.count();
        long newUsers = countUsersByCreatedAtBetween(startTime, endTime);
        long compareUsers = countUsersByCreatedAtBetween(compareStartTime, compareEndTime);
        
        // 计算用户增长率
        double userGrowth = calculateGrowthRate(newUsers, compareUsers);
        
        // 获取活跃用户数（简化实现，实际可能需要根据登录记录计算）
        // 这里假设最近登录的用户为活跃用户
        long activeUsers = countUsersByLastLoginTimeBetween(startTime, endTime);
        long compareActiveUsers = countUsersByLastLoginTimeBetween(compareStartTime, compareEndTime);
        
        // 计算活跃用户增长率
        double activeGrowth = calculateGrowthRate(activeUsers, compareActiveUsers);
        
        // 获取消息统计数据
        long totalMessages = messageRepository.count();
        long newMessages = countMessagesByCreatedAtBetween(startTime, endTime);
        long compareMessages = countMessagesByCreatedAtBetween(compareStartTime, compareEndTime);
        
        // 计算消息增长率
        double messageGrowth = calculateGrowthRate(newMessages, compareMessages);
        
        // 获取群组统计数据
        long totalGroups = groupRepository.count();
        long newGroups = countGroupsByCreatedAtBetween(startTime, endTime);
        long compareGroups = countGroupsByCreatedAtBetween(compareStartTime, compareEndTime);
        
        // 计算群组增长率
        double groupGrowth = calculateGrowthRate(newGroups, compareGroups);
        
        // 获取动态统计数据
        long totalMoments = momentRepository.count();
        long newMoments = countMomentsByCreatedAtBetween(startTime, endTime);
        long compareMoments = countMomentsByCreatedAtBetween(compareStartTime, compareEndTime);
        
        // 计算动态增长率
        double momentGrowth = calculateGrowthRate(newMoments, compareMoments);
        
        // 构建响应对象
        return SystemOverviewResponse.builder()
                .totalUsers(totalUsers)
                .userGrowth(userGrowth)
                .activeUsers(activeUsers)
                .activeGrowth(activeGrowth)
                .totalMessages(totalMessages)
                .messageGrowth(messageGrowth)
                .totalGroups(totalGroups)
                .groupGrowth(groupGrowth)
                .totalMoments(totalMoments)
                .momentGrowth(momentGrowth)
                .newUsers(newUsers)
                .period(period)
                .build();
    }

    /**
     * 获取统计数据
     * @param type 统计类型 (users, messages, groups, moments)
     * @param period 时间周期 (today, week, month, year)
     * @return 统计数据
     */
    @Override
    public StatisticsResponse getStatistics(String type, String period) {
        log.info("获取统计数据, type={}, period={}", type, period);
        
        // 校验统计类型
        if (!isValidStatisticsType(type)) {
            throw new IllegalArgumentException("无效的统计类型: " + type);
        }
        
        // 获取统计周期的起止时间
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 获取时间间隔和格式
        List<LocalDateTime> timePoints = getTimePointsForPeriod(period);
        DateTimeFormatter formatter = getFormatterForPeriod(period);
        
        // 生成时间标签
        List<String> labels = timePoints.stream()
                .map(tp -> tp.format(formatter))
                .collect(Collectors.toList());
        
        // 根据不同类型生成统计数据
        List<Number> data = new ArrayList<>();
        Long total = 0L;
        Double growthRate = 0.0;
        
        switch (type) {
            case "users":
                data = getUserCountByTimePoints(timePoints);
                total = userRepository.count();
                growthRate = calculateUserGrowthRate(period);
                break;
            case "messages":
                data = getMessageCountByTimePoints(timePoints);
                total = messageRepository.count();
                growthRate = calculateMessageGrowthRate(period);
                break;
            case "groups":
                data = getGroupCountByTimePoints(timePoints);
                total = groupRepository.count();
                growthRate = calculateGroupGrowthRate(period);
                break;
            case "moments":
                data = getMomentCountByTimePoints(timePoints);
                total = momentRepository.count();
                growthRate = calculateMomentGrowthRate(period);
                break;
        }
        
        // 构建响应对象
        return StatisticsResponse.builder()
                .type(type)
                .period(period)
                .labels(labels)
                .data(data)
                .total(total)
                .growthRate(growthRate)
                .build();
    }

    /**
     * 获取用户活跃度分布数据
     * @param period 时间周期 (today, week, month, year)
     * @return 用户活跃度分布数据
     */
    @Override
    public Map<String, Object> getUserActivityDistribution(String period) {
        log.info("获取用户活跃度分布数据, period={}", period);
        
        // 获取统计周期的起止时间
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 获取活跃用户按时间段分布
        Map<String, Long> timeDistribution = getActivityTimeDistribution(startTime, endTime);
        
        // 获取活跃用户按设备类型分布
        Map<String, Long> deviceDistribution = getActivityDeviceDistribution(startTime, endTime);
        
        // 获取活跃用户按操作类型分布
        Map<String, Long> actionDistribution = getActivityActionDistribution(startTime, endTime);
        
        // 构建响应对象
        Map<String, Object> result = new HashMap<>();
        result.put("timeDistribution", timeDistribution);
        result.put("deviceDistribution", deviceDistribution);
        result.put("actionDistribution", actionDistribution);
        result.put("period", period);
        
        return result;
    }
    
    /**
     * 获取内容类型分布数据
     * @param period 时间周期 (today, week, month, year)
     * @return 内容类型分布数据
     */
    @Override
    public Map<String, Object> getContentTypeDistribution(String period) {
        log.info("获取内容类型分布数据, period={}", period);
        
        // 获取统计周期的起止时间
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> distribution = new LinkedHashMap<>();
        
        try {
            // 文本消息
            Query textQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'TEXT'");
            textQuery.setParameter("start", startTime);
            textQuery.setParameter("end", endTime);
            distribution.put("text", (Long) textQuery.getSingleResult());
            
            // 图片消息
            Query imageQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'IMAGE'");
            imageQuery.setParameter("start", startTime);
            imageQuery.setParameter("end", endTime);
            distribution.put("image", (Long) imageQuery.getSingleResult());
            
            // 视频消息
            Query videoQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'VIDEO'");
            videoQuery.setParameter("start", startTime);
            videoQuery.setParameter("end", endTime);
            distribution.put("video", (Long) videoQuery.getSingleResult());
            
            // 文件消息
            Query fileQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'FILE'");
            fileQuery.setParameter("start", startTime);
            fileQuery.setParameter("end", endTime);
            distribution.put("file", (Long) fileQuery.getSingleResult());
            
            // 语音消息
            Query voiceQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'VOICE'");
            voiceQuery.setParameter("start", startTime);
            voiceQuery.setParameter("end", endTime);
            distribution.put("voice", (Long) voiceQuery.getSingleResult());
            
            // 系统消息
            Query systemQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'SYSTEM'");
            systemQuery.setParameter("start", startTime);
            systemQuery.setParameter("end", endTime);
            distribution.put("system", (Long) systemQuery.getSingleResult());
            
            // 其他类型消息
            Query otherQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM com.im.imcommunicationsystem.message.entity.Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType NOT IN ('TEXT', 'IMAGE', 'VIDEO', 'FILE', 'VOICE', 'SYSTEM')");
            otherQuery.setParameter("start", startTime);
            otherQuery.setParameter("end", endTime);
            distribution.put("other", (Long) otherQuery.getSingleResult());
        } catch (Exception e) {
            log.error("获取内容类型分布失败", e);
            // 在查询失败的情况下提供空数据
            distribution.put("text", 0L);
            distribution.put("image", 0L);
            distribution.put("video", 0L);
            distribution.put("file", 0L);
            distribution.put("voice", 0L);
            distribution.put("system", 0L);
            distribution.put("other", 0L);
        }
        
        result.put("distribution", distribution);
        result.put("period", period);
        
        // 计算总数
        Long total = distribution.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);
        
        return result;
    }
    
    // 自定义方法：根据时间范围统计用户数量
    private long countUsersByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Query query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end");
        query.setParameter("start", start);
        query.setParameter("end", end);
        return (Long) query.getSingleResult();
    }
    
    // 自定义方法：根据最后登录时间范围统计用户数量
    private long countUsersByLastLoginTimeBetween(LocalDateTime start, LocalDateTime end) {
        // 由于User实体中没有lastLoginTime字段，我们可以使用updatedAt字段作为近似的活跃时间
        Query query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end");
        query.setParameter("start", start);
        query.setParameter("end", end);
        return (Long) query.getSingleResult();
    }
    
    // 自定义方法：根据创建时间范围统计消息数量
    private long countMessagesByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Query query = entityManager.createQuery(
            "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end");
        query.setParameter("start", start);
        query.setParameter("end", end);
        return (Long) query.getSingleResult();
    }
    
    // 自定义方法：根据创建时间范围统计群组数量
    private long countGroupsByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Query query = entityManager.createQuery(
            "SELECT COUNT(g) FROM com.im.imcommunicationsystem.group.entity.Group g WHERE g.createdAt BETWEEN :start AND :end");
        query.setParameter("start", start);
        query.setParameter("end", end);
        return (Long) query.getSingleResult();
    }
    
    // 自定义方法：根据创建时间范围统计动态数量
    private long countMomentsByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Query query = entityManager.createQuery(
            "SELECT COUNT(m) FROM Moment m WHERE m.createdAt BETWEEN :start AND :end");
        query.setParameter("start", start);
        query.setParameter("end", end);
        return (Long) query.getSingleResult();
    }
    
    // 辅助方法：获取活跃用户按时间段分布
    private Map<String, Long> getActivityTimeDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> result = new LinkedHashMap<>();
        
        // 按照一天中的时间段统计用户活跃度
        // 早上: 6-12点
        Query morningQuery = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end " +
            "AND FUNCTION('HOUR', u.updatedAt) >= 6 AND FUNCTION('HOUR', u.updatedAt) < 12");
        morningQuery.setParameter("start", start);
        morningQuery.setParameter("end", end);
        result.put("morning", (Long) morningQuery.getSingleResult());
        
        // 下午: 12-18点
        Query afternoonQuery = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end " +
            "AND FUNCTION('HOUR', u.updatedAt) >= 12 AND FUNCTION('HOUR', u.updatedAt) < 18");
        afternoonQuery.setParameter("start", start);
        afternoonQuery.setParameter("end", end);
        result.put("afternoon", (Long) afternoonQuery.getSingleResult());
        
        // 晚上: 18-24点
        Query eveningQuery = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end " +
            "AND FUNCTION('HOUR', u.updatedAt) >= 18 AND FUNCTION('HOUR', u.updatedAt) < 24");
        eveningQuery.setParameter("start", start);
        eveningQuery.setParameter("end", end);
        result.put("evening", (Long) eveningQuery.getSingleResult());
        
        // 凌晨: 0-6点
        Query nightQuery = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end " +
            "AND FUNCTION('HOUR', u.updatedAt) >= 0 AND FUNCTION('HOUR', u.updatedAt) < 6");
        nightQuery.setParameter("start", start);
        nightQuery.setParameter("end", end);
        result.put("night", (Long) nightQuery.getSingleResult());
        
        return result;
    }
    
    // 辅助方法：获取活跃用户按设备类型分布
    private Map<String, Long> getActivityDeviceDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> result = new LinkedHashMap<>();
        
        try {
            // 从消息元数据中解析设备类型
            // 假设元数据中存储了发送设备信息
            
            // 获取消息总数作为基础
            Query totalQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end");
            totalQuery.setParameter("start", start);
            totalQuery.setParameter("end", end);
            Long totalMessages = (Long) totalQuery.getSingleResult();
            
            // 尝试从消息元数据中筛选Android设备消息 (假设元数据中有设备信息)
            Query androidQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.metadata LIKE '%android%'");
            androidQuery.setParameter("start", start);
            androidQuery.setParameter("end", end);
            Long androidCount = (Long) androidQuery.getSingleResult();
            result.put("android", androidCount);
            
            // 尝试从消息元数据中筛选iOS设备消息
            Query iosQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND (m.metadata LIKE '%ios%' OR m.metadata LIKE '%iPhone%' OR m.metadata LIKE '%iPad%')");
            iosQuery.setParameter("start", start);
            iosQuery.setParameter("end", end);
            Long iosCount = (Long) iosQuery.getSingleResult();
            result.put("ios", iosCount);
            
            // 尝试从消息元数据中筛选Web设备消息
            Query webQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.metadata LIKE '%web%'");
            webQuery.setParameter("start", start);
            webQuery.setParameter("end", end);
            Long webCount = (Long) webQuery.getSingleResult();
            result.put("web", webCount);
            
            // 将剩余部分归为桌面应用
            Long desktopCount = totalMessages - androidCount - iosCount - webCount;
            if (desktopCount < 0) desktopCount = 0L; // 防止出现负数
            result.put("desktop", desktopCount);
            
            // 验证数据是否合理，如果没有获取到任何数据，使用合理的默认分布
            Long totalTracked = androidCount + iosCount + webCount + desktopCount;
            if (totalTracked == 0) {
                throw new Exception("未能从元数据中获取有效的设备类型统计");
            }
        } catch (Exception e) {
            log.warn("设备类型统计失败，将使用基于消息类型的估算: {}", e.getMessage());
            
            // 根据消息类型进行估算
            // 文本消息可能主要来自移动设备
            Query textMessageQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'TEXT'");
            textMessageQuery.setParameter("start", start);
            textMessageQuery.setParameter("end", end);
            Long textMessages = (Long) textMessageQuery.getSingleResult();
            
            // 媒体消息(图片、视频)可能主要来自移动设备
            Query mediaMessageQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND (m.messageType = 'IMAGE' OR m.messageType = 'VIDEO')");
            mediaMessageQuery.setParameter("start", start);
            mediaMessageQuery.setParameter("end", end);
            Long mediaMessages = (Long) mediaMessageQuery.getSingleResult();
            
            // 文件消息可能主要来自桌面设备
            Query fileMessageQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType = 'FILE'");
            fileMessageQuery.setParameter("start", start);
            fileMessageQuery.setParameter("end", end);
            Long fileMessages = (Long) fileMessageQuery.getSingleResult();
            
            // 其他类型消息
            Query otherMessageQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end " +
                "AND m.messageType NOT IN ('TEXT', 'IMAGE', 'VIDEO', 'FILE')");
            otherMessageQuery.setParameter("start", start);
            otherMessageQuery.setParameter("end", end);
            Long otherMessages = (Long) otherMessageQuery.getSingleResult();
            
            // 根据消息类型分布来估算设备分布
            Long totalMessages = textMessages + mediaMessages + fileMessages + otherMessages;
            
            // 70%文本和媒体消息来自移动设备(分配给Android和iOS)
            Long mobileMessages = (textMessages + mediaMessages) * 70 / 100;
            result.put("android", mobileMessages * 60 / 100); // Android约占移动消息的60%
            result.put("ios", mobileMessages * 40 / 100);     // iOS约占移动消息的40%
            
            // 80%文件消息来自桌面设备
            Long desktopFileMessages = fileMessages * 80 / 100;
            
            // 将剩余消息平均分配
            Long remainingMessages = totalMessages - mobileMessages - desktopFileMessages;
            
            // 分配剩余消息
            result.put("web", remainingMessages / 2);
            result.put("desktop", desktopFileMessages + remainingMessages / 2);
        }
        
        return result;
    }
    
    // 辅助方法：获取活跃用户按操作类型分布
    private Map<String, Long> getActivityActionDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> result = new LinkedHashMap<>();
        
        // 消息发送统计
        Query messageQuery = entityManager.createQuery(
            "SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :start AND :end");
        messageQuery.setParameter("start", start);
        messageQuery.setParameter("end", end);
        result.put("message", (Long) messageQuery.getSingleResult());
        
        // 动态发布统计
        Query momentQuery = entityManager.createQuery(
            "SELECT COUNT(m) FROM Moment m WHERE m.createdAt BETWEEN :start AND :end");
        momentQuery.setParameter("start", start);
        momentQuery.setParameter("end", end);
        result.put("moment", (Long) momentQuery.getSingleResult());
        
        // 群组活动统计 (以创建群组为例)
        Query groupQuery = entityManager.createQuery(
            "SELECT COUNT(g) FROM com.im.imcommunicationsystem.group.entity.Group g WHERE g.createdAt BETWEEN :start AND :end");
        groupQuery.setParameter("start", start);
        groupQuery.setParameter("end", end);
        result.put("group", (Long) groupQuery.getSingleResult());
        
        // 个人资料更新统计
        Query profileQuery = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.updatedAt BETWEEN :start AND :end " +
            "AND u.createdAt != u.updatedAt"); // 确保是更新而不是创建
        profileQuery.setParameter("start", start);
        profileQuery.setParameter("end", end);
        result.put("profile", (Long) profileQuery.getSingleResult());
        
        return result;
    }
    
    // 辅助方法：计算增长率
    private double calculateGrowthRate(long current, long previous) {
        if (previous == 0) {
            // 如果前期值为0，而当前值不为0，则增长率为100%（而不是无穷大）
            return current > 0 ? 100.0 : 0.0;
        }
        // 计算增长百分比 = (当前值 - 前期值) / 前期值 * 100%
        return ((double) (current - previous) / previous) * 100.0;
    }
    
    // 辅助方法：验证统计类型是否有效
    private boolean isValidStatisticsType(String type) {
        return "users".equals(type) || 
               "messages".equals(type) || 
               "groups".equals(type) || 
               "moments".equals(type);
    }
    
    // 辅助方法：根据时间周期获取起始时间
    private LocalDateTime getStartTimeForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (period) {
            case "today":
                return now.toLocalDate().atStartOfDay();
            case "week":
                return now.toLocalDate().minusDays(6).atStartOfDay();
            case "month":
                return now.toLocalDate().minusDays(29).atStartOfDay();
            case "year":
                return now.toLocalDate().minusMonths(11).withDayOfMonth(1).atStartOfDay();
            default:
                return now.toLocalDate().atStartOfDay();
        }
    }
    
    // 辅助方法：根据时间周期获取对比周期的起始时间
    private LocalDateTime getStartTimeForComparePeriod(String period) {
        LocalDateTime startTime = getStartTimeForPeriod(period);
        
        switch (period) {
            case "today":
                return startTime.minusDays(1);
            case "week":
                return startTime.minusDays(7);
            case "month":
                return startTime.minusMonths(1);
            case "year":
                return startTime.minusYears(1);
            default:
                return startTime.minusDays(1);
        }
    }
    
    // 辅助方法：获取时间周期的时间点列表
    private List<LocalDateTime> getTimePointsForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> timePoints = new ArrayList<>();
        
        switch (period) {
            case "today":
                // 每小时一个点
                for (int i = 0; i < 24; i++) {
                    timePoints.add(now.toLocalDate().atStartOfDay().plusHours(i));
                }
                break;
            case "week":
                // 每天一个点
                for (int i = 0; i < 7; i++) {
                    timePoints.add(now.toLocalDate().minusDays(6).plusDays(i).atStartOfDay());
                }
                break;
            case "month":
                // 每天一个点
                for (int i = 0; i < 30; i++) {
                    timePoints.add(now.toLocalDate().minusDays(29).plusDays(i).atStartOfDay());
                }
                break;
            case "year":
                // 每月一个点
                for (int i = 0; i < 12; i++) {
                    timePoints.add(now.toLocalDate().minusMonths(11).plusMonths(i).withDayOfMonth(1).atStartOfDay());
                }
                break;
            default:
                // 默认每天一个点
                for (int i = 0; i < 7; i++) {
                    timePoints.add(now.toLocalDate().minusDays(6).plusDays(i).atStartOfDay());
                }
        }
        
        return timePoints;
    }
    
    // 辅助方法：获取时间格式化器
    private DateTimeFormatter getFormatterForPeriod(String period) {
        switch (period) {
            case "today":
                return DateTimeFormatter.ofPattern("HH:00");
            case "week":
            case "month":
                return DateTimeFormatter.ofPattern("MM-dd");
            case "year":
                return DateTimeFormatter.ofPattern("yyyy-MM");
            default:
                return DateTimeFormatter.ofPattern("MM-dd");
        }
    }
    
    // 辅助方法：根据时间点获取用户数量
    private List<Number> getUserCountByTimePoints(List<LocalDateTime> timePoints) {
        List<Number> result = new ArrayList<>();
        
        for (int i = 0; i < timePoints.size(); i++) {
            LocalDateTime start = timePoints.get(i);
            LocalDateTime end = i < timePoints.size() - 1 ? 
                    timePoints.get(i + 1) : LocalDateTime.now();
            
            long count = countUsersByCreatedAtBetween(start, end);
            result.add(count);
        }
        
        return result;
    }
    
    // 辅助方法：根据时间点获取消息数量
    private List<Number> getMessageCountByTimePoints(List<LocalDateTime> timePoints) {
        List<Number> result = new ArrayList<>();
        
        for (int i = 0; i < timePoints.size(); i++) {
            LocalDateTime start = timePoints.get(i);
            LocalDateTime end = i < timePoints.size() - 1 ? 
                    timePoints.get(i + 1) : LocalDateTime.now();
            
            long count = countMessagesByCreatedAtBetween(start, end);
            result.add(count);
        }
        
        return result;
    }
    
    // 辅助方法：根据时间点获取群组数量
    private List<Number> getGroupCountByTimePoints(List<LocalDateTime> timePoints) {
        List<Number> result = new ArrayList<>();
        
        for (int i = 0; i < timePoints.size(); i++) {
            LocalDateTime start = timePoints.get(i);
            LocalDateTime end = i < timePoints.size() - 1 ? 
                    timePoints.get(i + 1) : LocalDateTime.now();
            
            long count = countGroupsByCreatedAtBetween(start, end);
            result.add(count);
        }
        
        return result;
    }
    
    // 辅助方法：根据时间点获取动态数量
    private List<Number> getMomentCountByTimePoints(List<LocalDateTime> timePoints) {
        List<Number> result = new ArrayList<>();
        
        for (int i = 0; i < timePoints.size(); i++) {
            LocalDateTime start = timePoints.get(i);
            LocalDateTime end = i < timePoints.size() - 1 ? 
                    timePoints.get(i + 1) : LocalDateTime.now();
            
            long count = countMomentsByCreatedAtBetween(start, end);
            result.add(count);
        }
        
        return result;
    }
    
    // 辅助方法：计算用户增长率
    private double calculateUserGrowthRate(String period) {
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime compareStartTime = getStartTimeForComparePeriod(period);
        LocalDateTime compareEndTime = startTime;
        
        long current = countUsersByCreatedAtBetween(startTime, endTime);
        long previous = countUsersByCreatedAtBetween(compareStartTime, compareEndTime);
        
        return calculateGrowthRate(current, previous);
    }
    
    // 辅助方法：计算消息增长率
    private double calculateMessageGrowthRate(String period) {
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime compareStartTime = getStartTimeForComparePeriod(period);
        LocalDateTime compareEndTime = startTime;
        
        long current = countMessagesByCreatedAtBetween(startTime, endTime);
        long previous = countMessagesByCreatedAtBetween(compareStartTime, compareEndTime);
        
        return calculateGrowthRate(current, previous);
    }
    
    // 辅助方法：计算群组增长率
    private double calculateGroupGrowthRate(String period) {
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime compareStartTime = getStartTimeForComparePeriod(period);
        LocalDateTime compareEndTime = startTime;
        
        long current = countGroupsByCreatedAtBetween(startTime, endTime);
        long previous = countGroupsByCreatedAtBetween(compareStartTime, compareEndTime);
        
        return calculateGrowthRate(current, previous);
    }
    
    // 辅助方法：计算动态增长率
    private double calculateMomentGrowthRate(String period) {
        LocalDateTime startTime = getStartTimeForPeriod(period);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime compareStartTime = getStartTimeForComparePeriod(period);
        LocalDateTime compareEndTime = startTime;
        
        long current = countMomentsByCreatedAtBetween(startTime, endTime);
        long previous = countMomentsByCreatedAtBetween(compareStartTime, compareEndTime);
        
        return calculateGrowthRate(current, previous);
    }
} 