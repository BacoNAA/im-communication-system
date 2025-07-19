package com.im.imcommunicationsystem.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布公告请求DTO
 * 用于接收发布公告的请求参数
 * 
 * @author IM System
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishAnnouncementRequest {

    /**
     * 公告标题
     */
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 1, max = 100, message = "公告标题长度必须在1-100之间")
    private String title;

    /**
     * 公告内容
     */
    @NotBlank(message = "公告内容不能为空")
    @Size(min = 1, max = 5000, message = "公告内容长度必须在1-5000之间")
    private String content;

    /**
     * 是否置顶
     */
    private Boolean isPinned;
} 