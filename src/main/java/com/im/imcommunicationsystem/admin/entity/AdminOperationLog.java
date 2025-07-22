package com.im.imcommunicationsystem.admin.entity;

import com.im.imcommunicationsystem.admin.enums.OperationType;
import com.im.imcommunicationsystem.admin.enums.TargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 */
@Entity
@Table(name = "admin_operation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOperationLog {

    /**
     * 日志ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 管理员ID
     */
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /**
     * 操作类型
     * 使用字符串类型存储，但在实体类中使用枚举类型
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationTypeString;

    /**
     * 目标类型
     * 使用字符串类型存储，但在实体类中使用枚举类型
     */
    @Column(name = "target_type", length = 50)
    private String targetTypeString;

    /**
     * 目标ID
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 操作描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 获取操作类型枚举
     * @return 操作类型枚举
     */
    @Transient
    public OperationType getOperationType() {
        if (operationTypeString == null) {
            return null;
        }
        try {
            // 尝试直接使用字符串值
            return OperationType.valueOf(operationTypeString);
        } catch (IllegalArgumentException e) {
            // 如果失败，可能是大小写问题
            for (OperationType type : OperationType.values()) {
                if (type.name().equalsIgnoreCase(operationTypeString)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 设置操作类型枚举
     * @param operationType 操作类型枚举
     */
    public void setOperationType(OperationType operationType) {
        this.operationTypeString = operationType != null ? operationType.name() : null;
    }

    /**
     * 获取目标类型枚举
     * @return 目标类型枚举
     */
    @Transient
    public TargetType getTargetType() {
        if (targetTypeString == null) {
            return null;
        }
        try {
            // 尝试直接使用字符串值
            return TargetType.valueOf(targetTypeString);
        } catch (IllegalArgumentException e) {
            // 如果失败，可能是大小写问题
            for (TargetType type : TargetType.values()) {
                if (type.name().equalsIgnoreCase(targetTypeString)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 设置目标类型枚举
     * @param targetType 目标类型枚举
     */
    public void setTargetType(TargetType targetType) {
        this.targetTypeString = targetType != null ? targetType.name() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 