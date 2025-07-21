package com.im.imcommunicationsystem.moment.utils;

import com.im.imcommunicationsystem.moment.dto.response.MomentResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 时间线工具类
 */
@Slf4j
public class TimelineUtils {

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 合并多个用户的动态时间线
     *
     * @param momentsList 多个用户的动态列表
     * @param pageSize 分页大小
     * @return 合并排序后的动态列表
     */
    public static List<MomentResponse> mergeMomentTimelines(List<List<MomentResponse>> momentsList, int pageSize) {
        if (momentsList == null || momentsList.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<MomentResponse> mergedList = new ArrayList<>();
        
        // 合并所有列表
        for (List<MomentResponse> moments : momentsList) {
            if (moments != null && !moments.isEmpty()) {
                mergedList.addAll(moments);
            }
        }
        
        // 按创建时间降序排序
        mergedList.sort((m1, m2) -> {
            LocalDateTime time1 = m1.getCreatedAt();
            LocalDateTime time2 = m2.getCreatedAt();
            return time2.compareTo(time1); // 降序
        });
        
        // 限制返回数量
        if (mergedList.size() > pageSize) {
            return mergedList.subList(0, pageSize);
        } else {
            return mergedList;
        }
    }
    
    /**
     * 计算时间线排序分数
     * 计算规则：基于时间的新鲜度，加上热度（点赞+评论）的权重
     *
     * @param createdAt 创建时间
     * @param likeCount 点赞数
     * @param commentCount 评论数
     * @return 排序分数
     */
    public static double calculateTimelineScore(LocalDateTime createdAt, int likeCount, int commentCount) {
        // 基于时间的新鲜度
        long hoursAgo = Duration.between(createdAt, LocalDateTime.now()).toHours();
        double freshnessScore = Math.max(0, 100 - Math.min(hoursAgo, 72) * 1.0); // 最多考虑3天内的新鲜度
        
        // 热度计算
        double engagementScore = (likeCount * 1.0) + (commentCount * 2.0); // 评论权重高于点赞
        
        // 总分 = 新鲜度 * 0.7 + 热度 * 0.3
        return freshnessScore * 0.7 + engagementScore * 0.3;
    }
    
    /**
     * 格式化相对时间显示
     *
     * @param dateTime 日期时间
     * @return 相对时间字符串
     */
    public static String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return duration.toMinutes() + "分钟前";
        } else if (seconds < 86400) {
            return duration.toHours() + "小时前";
        } else if (seconds < 2592000) { // 30天
            return duration.toDays() + "天前";
        } else if (seconds < 31536000) { // 365天
            return (duration.toDays() / 30) + "个月前";
        } else {
            return (duration.toDays() / 365) + "年前";
        }
    }
    
    /**
     * 格式化日期时间
     *
     * @param dateTime 日期时间
     * @return 格式化后的日期时间字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(FORMATTER);
    }
} 