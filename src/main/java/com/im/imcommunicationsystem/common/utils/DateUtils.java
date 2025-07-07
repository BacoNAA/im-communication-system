package com.im.imcommunicationsystem.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将 LocalDateTime 格式化为 "yyyy-MM-dd HH:mm:ss" 格式的字符串
     * @param dateTime LocalDateTime 对象
     * @return 格式化后的字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * 将 "yyyy-MM-dd HH:mm:ss" 格式的字符串解析为 LocalDateTime 对象
     * @param dateTimeStr 字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }
}