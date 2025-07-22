/**
 * 系统通知API模块
 */
import { api } from './request';
import type { ApiResponse } from './request';

/**
 * 系统通知响应接口
 */
export interface SystemNotificationResponse {
    /**
     * 通知ID
     */
    id: number;
    
    /**
     * 通知标题
     */
    title: string;
    
    /**
     * 通知内容摘要
     */
    summary: string;
    
    /**
     * 通知类型
     */
    type: string;
    
    /**
     * 是否已读
     */
    isRead: boolean;
    
    /**
     * 发布时间
     */
    publishedAt: string;
    
    /**
     * 创建时间
     */
    createdAt: string;
    
    /**
     * 阅读时间
     */
    readAt?: string;
}

/**
 * 系统通知详情响应接口
 */
export interface SystemNotificationDetailResponse {
    /**
     * 通知ID
     */
    id: number;
    
    /**
     * 通知标题
     */
    title: string;
    
    /**
     * 通知完整内容
     */
    content: string;
    
    /**
     * 通知类型
     */
    type: string;
    
    /**
     * 是否已读
     */
    isRead: boolean;
    
    /**
     * 发布时间
     */
    publishedAt: string;
    
    /**
     * 创建时间
     */
    createdAt: string;
    
    /**
     * 阅读时间
     */
    readAt?: string;
    
    /**
     * 创建者信息
     */
    createdBy: string;
}

/**
 * 分页响应接口
 */
export interface PageResponse<T> {
    /**
     * 当前页内容
     */
    content: T[];
    
    /**
     * 总页数
     */
    totalPages: number;
    
    /**
     * 总元素数
     */
    totalElements: number;
    
    /**
     * 当前页码
     */
    number: number;
    
    /**
     * 当前页大小
     */
    size: number;
    
    /**
     * 是否为第一页
     */
    first: boolean;
    
    /**
     * 是否为最后一页
     */
    last: boolean;
}

/**
 * 系统通知API
 */
export const notificationApi = {
    /**
     * 获取系统通知列表
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param unreadOnly 是否只获取未读通知
     * @returns 通知列表
     */
    async getNotifications(page: number = 0, size: number = 10, unreadOnly: boolean = false): Promise<ApiResponse<PageResponse<SystemNotificationResponse>>> {
        try {
            const response = await api.get<ApiResponse<PageResponse<SystemNotificationResponse>>>(`/notifications?page=${page}&size=${size}&unreadOnly=${unreadOnly}`);
            return response;
        } catch (error) {
            console.error('获取系统通知列表失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取系统通知详情
     * 
     * @param notificationId 通知ID
     * @returns 通知详情
     */
    async getNotificationDetail(notificationId: number): Promise<ApiResponse<SystemNotificationDetailResponse>> {
        try {
            const response = await api.get<ApiResponse<SystemNotificationDetailResponse>>(`/notifications/${notificationId}`);
            return response;
        } catch (error) {
            console.error('获取系统通知详情失败:', error);
            throw error;
        }
    },
    
    /**
     * 标记通知为已读
     * 
     * @param notificationId 通知ID
     * @returns 操作结果
     */
    async markAsRead(notificationId: number): Promise<ApiResponse<string>> {
        try {
            const response = await api.post<ApiResponse<string>>(`/notifications/${notificationId}/read`);
            return response;
        } catch (error) {
            console.error('标记通知为已读失败:', error);
            throw error;
        }
    },
    
    /**
     * 批量标记通知为已读
     * 
     * @param notificationIds 通知ID列表
     * @returns 操作结果
     */
    async markMultipleAsRead(notificationIds: number[]): Promise<ApiResponse<{updatedCount: number}>> {
        try {
            const response = await api.post<ApiResponse<{updatedCount: number}>>('/notifications/read-multiple', {
                notificationIds
            });
            return response;
        } catch (error) {
            console.error('批量标记通知为已读失败:', error);
            throw error;
        }
    },
    
    /**
     * 标记所有通知为已读
     * 
     * @returns 操作结果
     */
    async markAllAsRead(): Promise<ApiResponse<{updatedCount: number}>> {
        try {
            const response = await api.post<ApiResponse<{updatedCount: number}>>('/notifications/read-all');
            return response;
        } catch (error) {
            console.error('标记所有通知为已读失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取未读通知数量
     * 
     * @returns 未读通知数量
     */
    async getUnreadCount(): Promise<ApiResponse<{unreadCount: number}>> {
        try {
            const response = await api.get<ApiResponse<{unreadCount: number}>>('/notifications/unread-count');
            return response;
        } catch (error) {
            console.error('获取未读通知数量失败:', error);
            throw error;
        }
    }
};

export default notificationApi; 