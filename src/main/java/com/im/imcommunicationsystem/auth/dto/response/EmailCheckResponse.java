package com.im.imcommunicationsystem.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱检查响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckResponse {

    /**
     * 邮箱是否已存在
     */
    private boolean exists;
}