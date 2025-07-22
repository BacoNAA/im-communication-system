package com.im.imcommunicationsystem.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 举报处理请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportHandleRequest {

    /**
     * 要处理的举报ID
     */
    // 移除必填验证，因为reportId会从URL路径中获取
    private Long reportId;
    
    /**
     * 处理操作（process:处理中, resolve:已解决, reject:已拒绝）
     */
    @NotBlank(message = "处理操作不能为空")
    private String action;
    
    /**
     * 处理结果
     */
    @NotBlank(message = "处理结果不能为空")
    private String result;
    
    /**
     * 处理备注
     */
    private String note;
    
    /**
     * 对用户的操作（warn:警告, temporary_ban:临时封禁, permanent_ban:永久封禁, none:无, remove_member:移除群成员, ban_user:封禁用户）
     */
    private String userAction;
    
    /**
     * 对内容的操作（delete:删除, hide:隐藏, mark_as_sensitive:标记敏感, none:无, dissolve:解散群组, ban_group:封禁群组）
     */
    private String contentAction;
    
    /**
     * 封禁时长（小时），仅在临时封禁操作时使用
     */
    private Integer banDuration;
} 