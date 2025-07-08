package com.im.imcommunicationsystem.common.utils;

import com.im.imcommunicationsystem.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期时间工具类
 * 提供统一的日期时间处理功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class DateTimeUtils {

    /**
     * 预定义的日期时间格式化器
     */
    public static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern(CommonConstants.DateTime.STANDARD_FORMAT);
    
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_FORMAT);
    
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern(CommonConstants.DateTime.TIME_FORMAT);
    
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern(CommonConstants.DateTime.ISO_FORMAT);
    
    public static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern(CommonConstants.DateTime.FILENAME_DATE_FORMAT + CommonConstants.DateTime.FILENAME_TIME_FORMAT);

    /**
     * 默认时区
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 获取当前时间
     * 
     * @return 当前LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前日期时间
     * 
     * @return 当前LocalDateTime
     */
    public static LocalDateTime getCurrentDateTime() {
        return now();
    }

    /**
     * 获取当前日期时间字符串
     * 
     * @return 当前日期时间的字符串表示
     */
    public static String getCurrentDateTimeStr() {
        return format(now());
    }

    /**
     * 获取当前日期
     * 
     * @return 当前LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     * 
     * @return 时间戳
     */
    public static long currentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * LocalDateTime转换为时间戳（毫秒）
     * 
     * @param dateTime LocalDateTime
     * @return 时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 时间戳（毫秒）转换为LocalDateTime
     * 
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE);
    }

    /**
     * Date转换为LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * LocalDateTime转换为Date
     * 
     * @param dateTime LocalDateTime
     * @return Date对象
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * 格式化日期时间为默认格式
     * 
     * @param dateTime LocalDateTime
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DEFAULT_DATETIME_FORMATTER);
    }

    /**
     * 格式化日期时间为指定格式
     * 
     * @param dateTime LocalDateTime
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return "";
        }
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.warn("日期格式化失败: {}, pattern: {}", dateTime, pattern, e);
            return format(dateTime);
        }
    }

    /**
     * 格式化日期为默认格式
     * 
     * @param date LocalDate
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DEFAULT_DATE_FORMATTER);
    }

    /**
     * 格式化日期时间为指定格式
     * 
     * @param dateTime LocalDateTime
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String formatDate(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return "";
        }
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.warn("日期格式化失败: {}, pattern: {}", dateTime, pattern, e);
            return format(dateTime);
        }
    }

    /**
     * 格式化时间为默认格式
     * 
     * @param time LocalTime
     * @return 格式化后的字符串
     */
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DEFAULT_TIME_FORMATTER);
    }

    /**
     * 解析日期时间字符串
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DEFAULT_DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("日期时间解析失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 解析日期时间字符串（指定格式）
     * 
     * @param dateTimeStr 日期时间字符串
     * @param pattern 格式模式
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty() || pattern == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            log.warn("日期时间解析失败: {}, pattern: {}", dateTimeStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析日期字符串
     * 
     * @param dateStr 日期字符串
     * @return LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DEFAULT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("日期解析失败: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 计算两个日期时间之间的差值（天数）
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 天数差值
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
    }

    /**
     * 计算两个日期时间之间的差值（小时数）
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 小时数差值
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的差值（分钟数）
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 分钟数差值
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个日期时间之间的差值（秒数）
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 秒数差值
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 添加天数
     * 
     * @param dateTime 原始时间
     * @param days 天数
     * @return 新的时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * 添加小时
     * 
     * @param dateTime 原始时间
     * @param hours 小时数
     * @return 新的时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 添加分钟
     * 
     * @param dateTime 原始时间
     * @param minutes 分钟数
     * @return 新的时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 添加秒数
     * 
     * @param dateTime 原始时间
     * @param seconds 秒数
     * @return 新的时间
     */
    public static LocalDateTime plusSeconds(LocalDateTime dateTime, long seconds) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusSeconds(seconds);
    }

    /**
     * 获取一天的开始时间（00:00:00）
     * 
     * @param date 日期
     * @return 一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * 获取一天的结束时间（23:59:59.999999999）
     * 
     * @param date 日期
     * @return 一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取本周的开始时间（周一00:00:00）
     * 
     * @param date 日期
     * @return 本周的开始时间
     */
    public static LocalDateTime startOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(DayOfWeek.MONDAY).atStartOfDay();
    }

    /**
     * 获取本月的开始时间（1号00:00:00）
     * 
     * @param date 日期
     * @return 本月的开始时间
     */
    public static LocalDateTime startOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取本年的开始时间（1月1号00:00:00）
     * 
     * @param date 日期
     * @return 本年的开始时间
     */
    public static LocalDateTime startOfYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfYear(1).atStartOfDay();
    }

    /**
     * 判断是否为今天
     * 
     * @param dateTime 日期时间
     * @return 是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(today());
    }

    /**
     * 判断是否为昨天
     * 
     * @param dateTime 日期时间
     * @return 是否为昨天
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(today().minusDays(1));
    }

    /**
     * 判断是否为本周
     * 
     * @param dateTime 日期时间
     * @return 是否为本周
     */
    public static boolean isThisWeek(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate date = dateTime.toLocalDate();
        LocalDate today = today();
        LocalDate startOfWeek = startOfWeek(today).toLocalDate();
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek);
    }

    /**
     * 判断是否为本月
     * 
     * @param dateTime 日期时间
     * @return 是否为本月
     */
    public static boolean isThisMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate date = dateTime.toLocalDate();
        LocalDate today = today();
        return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
    }

    /**
     * 判断是否为本年
     * 
     * @param dateTime 日期时间
     * @return 是否为本年
     */
    public static boolean isThisYear(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.getYear() == today().getYear();
    }

    /**
     * 获取友好的时间显示
     * 
     * @param dateTime 日期时间
     * @return 友好的时间显示
     */
    public static String getFriendlyTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        LocalDateTime now = now();
        long seconds = secondsBetween(dateTime, now);
        
        if (seconds < 0) {
            return "未来时间";
        }
        
        if (seconds < 60) {
            return "刚刚";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }
        
        if (isYesterday(dateTime)) {
            return "昨天 " + formatTime(dateTime.toLocalTime());
        }
        
        if (isThisWeek(dateTime)) {
            String dayOfWeek = getDayOfWeekChinese(dateTime.getDayOfWeek());
            return dayOfWeek + " " + formatTime(dateTime.toLocalTime());
        }
        
        if (isThisYear(dateTime)) {
            return format(dateTime, "MM-dd HH:mm");
        }
        
        return format(dateTime, "yyyy-MM-dd HH:mm");
    }

    /**
     * 获取中文星期
     * 
     * @param dayOfWeek 星期枚举
     * @return 中文星期
     */
    private static String getDayOfWeekChinese(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }

    /**
     * 生成文件时间戳（用于文件名）
     * 
     * @return 文件时间戳
     */
    public static String generateFileTimestamp() {
        return now().format(FILE_TIMESTAMP_FORMATTER);
    }

    /**
     * 验证日期时间字符串格式
     * 
     * @param dateTimeStr 日期时间字符串
     * @param pattern 格式模式
     * @return 是否有效
     */
    public static boolean isValidDateTime(String dateTimeStr, String pattern) {
        return parse(dateTimeStr, pattern) != null;
    }

    /**
     * 获取时间范围描述
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return 时间范围描述
     */
    public static String getTimeRangeDescription(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "";
        }
        
        long days = daysBetween(start, end);
        if (days > 0) {
            return days + "天";
        }
        
        long hours = hoursBetween(start, end);
        if (hours > 0) {
            return hours + "小时";
        }
        
        long minutes = minutesBetween(start, end);
        if (minutes > 0) {
            return minutes + "分钟";
        }
        
        long seconds = secondsBetween(start, end);
        return seconds + "秒";
    }

    /**
     * 日期时间工具使用说明：
     * 
     * 1. 时间获取：
     *    - now() - 获取当前时间
     *    - today() - 获取当前日期
     *    - currentTimestamp() - 获取当前时间戳
     * 
     * 2. 时间转换：
     *    - toTimestamp() / fromTimestamp() - LocalDateTime与时间戳互转
     *    - toDate() / fromDate() - LocalDateTime与Date互转
     * 
     * 3. 时间格式化：
     *    - format() - 格式化为字符串
     *    - parse() - 解析字符串为时间
     *    - getFriendlyTime() - 友好时间显示
     * 
     * 4. 时间计算：
     *    - daysBetween() / hoursBetween() - 计算时间差
     *    - plusDays() / plusHours() - 时间加减
     *    - startOfDay() / endOfDay() - 获取时间边界
     * 
     * 5. 时间判断：
     *    - isToday() / isYesterday() - 判断是否为特定日期
     *    - isThisWeek() / isThisMonth() - 判断是否为特定时间段
     * 
     * 使用示例：
     * 
     * // 获取当前时间
     * LocalDateTime now = DateTimeUtils.now();
     * 
     * // 格式化时间
     * String timeStr = DateTimeUtils.format(now);
     * 
     * // 计算时间差
     * long hours = DateTimeUtils.hoursBetween(startTime, endTime);
     * 
     * // 友好时间显示
     * String friendlyTime = DateTimeUtils.getFriendlyTime(messageTime);
     * 
     * // 生成文件时间戳
     * String fileTimestamp = DateTimeUtils.generateFileTimestamp();
     * 
     * 注意事项：
     * 1. 所有方法都使用系统默认时区（Asia/Shanghai）
     * 2. 空值处理：输入为null时返回null或默认值
     * 3. 异常处理：解析失败时返回null并记录日志
     * 4. 性能优化：预定义格式化器，避免重复创建
     * 5. 线程安全：所有方法都是静态的，线程安全
     */

}