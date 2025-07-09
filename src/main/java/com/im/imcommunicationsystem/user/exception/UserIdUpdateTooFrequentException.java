package com.im.imcommunicationsystem.user.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 个人ID修改过于频繁异常
 * 用于处理用户修改个人ID过于频繁的情况
 */
public class UserIdUpdateTooFrequentException extends RuntimeException {

    private final LocalDateTime lastUpdateTime;
    private final LocalDateTime nextAllowedTime;
    private final long intervalSeconds;

    public UserIdUpdateTooFrequentException(LocalDateTime lastUpdateTime, LocalDateTime nextAllowedTime, long intervalSeconds) {
        super(createMessage(lastUpdateTime, nextAllowedTime, intervalSeconds));
        this.lastUpdateTime = lastUpdateTime;
        this.nextAllowedTime = nextAllowedTime;
        this.intervalSeconds = intervalSeconds;
    }

    private static String createMessage(LocalDateTime lastUpdateTime, LocalDateTime nextAllowedTime, long intervalSeconds) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
            "个人ID修改过于频繁。上次修改时间：%s，需要间隔%d秒才能再次修改，请在%s之后重试。",
            lastUpdateTime.format(formatter),
            intervalSeconds,
            nextAllowedTime.format(formatter)
        );
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public LocalDateTime getNextAllowedTime() {
        return nextAllowedTime;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }
}