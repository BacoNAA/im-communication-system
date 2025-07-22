package com.im.imcommunicationsystem.admin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员ID请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminIdRequest {

    /**
     * 管理员ID
     */
    private String adminId;
} 