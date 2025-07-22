import { api } from '@/api/request';
import type { ApiResponse } from '@/types';

/**
 * 创建举报请求接口
 */
export interface CreateReportRequest {
    /**
     * 被举报用户ID（可选）
     */
    reportedUserId?: number;
    
    /**
     * 被举报内容类型（必填）
     * 例如：USER、MESSAGE、GROUP、MOMENT等
     */
    reportedContentType: string;
    
    /**
     * 被举报内容ID（必填）
     */
    reportedContentId: number;
    
    /**
     * 举报原因（必填）
     * 例如：垃圾信息、色情内容、暴力内容、诈骗信息等
     */
    reason: string;
    
    /**
     * 举报详细描述（可选）
     */
    description?: string;
}

/**
 * 举报信息接口
 */
export interface ReportResponse {
    /**
     * 举报ID
     */
    reportId: number;
    
    /**
     * 举报者ID
     */
    reporterId: number;
    
    /**
     * 举报者用户名
     */
    reporterUsername: string;
    
    /**
     * 被举报用户ID（如适用）
     */
    reportedUserId?: number;
    
    /**
     * 被举报用户名（如适用）
     */
    reportedUsername?: string;
    
    /**
     * 被举报内容类型
     */
    reportedContentType: string;
    
    /**
     * 被举报内容ID
     */
    reportedContentId: number;
    
    /**
     * 举报原因
     */
    reason: string;
    
    /**
     * 举报详细描述
     */
    description?: string;
    
    /**
     * 举报状态
     */
    status: string;
    
    /**
     * 创建时间
     */
    createdAt: string;
    
    /**
     * 处理时间
     */
    handledAt?: string;
    
    /**
     * 处理人ID
     */
    handledBy?: number;
}

/**
 * 举报内容详情接口
 */
export interface ReportContentDetails {
    // 通用字段
    id: number;
    content?: string;
    createdAt?: string;
    
    // 用户相关字段
    username?: string;
    nickname?: string;
    email?: string;
    avatar?: string;
    
    // 消息相关字段
    senderId?: number;
    senderName?: string;
    conversationId?: number;
    messageType?: string;
    
    // 群组相关字段
    name?: string;
    description?: string;
    memberCount?: number;
    
    // 动态相关字段
    userId?: number;
    text?: string;
    mediaUrls?: string[];
    likeCount?: number;
    commentCount?: number;
    
    // 其他可能的字段
    [key: string]: any;
}

/**
 * 举报API模块
 */
export const reportApi = {
    /**
     * 提交举报
     *
     * @param request 举报请求
     * @returns 提交结果
     */
    async submitReport(request: CreateReportRequest): Promise<ApiResponse<ReportResponse>> {
        try {
            console.log('提交举报:', request);
            const response = await api.post<ApiResponse<ReportResponse>>('/reports', request);
            console.log('举报提交成功:', response);
            return response;
        } catch (error) {
            console.error('举报提交失败:', error);
            throw error;
        }
    },
    
    /**
     * 举报联系人
     * 
     * @param contactId 联系人ID
     * @param reporterId 举报者ID
     * @param reason 举报原因
     * @param description 举报详细描述（可选）
     * @returns 举报结果
     */
    async reportContact(contactId: number, reporterId: number, reason: string, description?: string): Promise<ApiResponse<ReportResponse>> {
        try {
            const request: CreateReportRequest = {
                reportedContentType: 'USER',
                reportedContentId: contactId,
                reportedUserId: contactId,
                reason: reason
            };
            
            // 只在description有值时添加到请求中
            if (description) {
                request.description = description;
            }
            
            console.log('举报联系人:', request);
            return this.submitReport(request);
        } catch (error) {
            console.error('举报联系人失败:', error);
            throw error;
        }
    },
    
    /**
     * 举报群组成员
     * 
     * @param memberId 成员ID
     * @param groupId 群组ID
     * @param reporterId 举报者ID
     * @param reason 举报原因
     * @param description 举报详细描述（可选）
     * @returns 举报结果
     */
    async reportGroupMember(memberId: number, groupId: number, reporterId: number, reason: string, description?: string): Promise<ApiResponse<ReportResponse>> {
        try {
            const request: CreateReportRequest = {
                reportedContentType: 'GROUP_MEMBER',
                reportedContentId: memberId,
                reportedUserId: memberId,
                reason: reason
            };
            
            // 只在description有值时添加到请求中
            if (description) {
                request.description = description;
            }
            
            console.log('举报群组成员:', request);
            return this.submitReport(request);
        } catch (error) {
            console.error('举报群组成员失败:', error);
            throw error;
        }
    },
    
    /**
     * 举报群组
     * 
     * @param groupId 群组ID
     * @param reporterId 举报者ID
     * @param reason 举报原因
     * @param description 举报详细描述（可选）
     * @returns 举报结果
     */
    async reportGroup(groupId: number, reporterId: number, reason: string, description?: string): Promise<ApiResponse<ReportResponse>> {
        try {
            const request: CreateReportRequest = {
                reportedContentType: 'GROUP',
                reportedContentId: groupId,
                reason: reason
            };
            
            // 只在description有值时添加到请求中
            if (description) {
                request.description = description;
            }
            
            console.log('举报群组:', request);
            return this.submitReport(request);
        } catch (error) {
            console.error('举报群组失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取举报列表（仅管理员使用）
     *
     * @param page 页码，从0开始
     * @param size 每页大小
     * @param status 状态过滤（可选）
     * @param contentType 内容类型过滤（可选）
     * @returns 举报列表
     */
    async getReportList(page: number = 0, size: number = 10, status?: string, contentType?: string): Promise<ApiResponse<Page<ReportResponse>>> {
        try {
            // 构建URL参数
            const params = new URLSearchParams();
            params.append('page', String(page));
            params.append('size', String(size));
            
            if (status) {
                params.append('status', status);
            }
            
            if (contentType) {
                params.append('contentType', contentType);
            }
            
            console.log('获取举报列表:', { page, size, status, contentType });
            const response = await api.get<ApiResponse<Page<ReportResponse>>>(`/admin/reports?${params.toString()}`);
            console.log('获取举报列表成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报列表失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取举报详情（仅管理员使用）
     *
     * @param reportId 举报ID
     * @returns 举报详情
     */
    async getReportDetails(reportId: number): Promise<ApiResponse<ReportResponse>> {
        try {
            console.log('获取举报详情:', reportId);
            const response = await api.get<ApiResponse<ReportResponse>>(`/admin/reports/${reportId}`);
            console.log('获取举报详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报详情失败:', error);
            throw error;
        }
    },
    
    /**
     * 处理举报（仅管理员使用）
     *
     * @param reportId 举报ID
     * @param action 操作（process, resolve, reject）
     * @param result 处理结果
     * @param note 处理备注
     * @param userAction 对用户的操作
     * @param contentAction 对内容的操作
     * @returns 处理结果
     */
    async handleReport(reportId: number, action: string, result: string, note?: string, userAction?: string, contentAction?: string): Promise<ApiResponse<ReportResponse>> {
        try {
            const request = {
                reportId,
                action,
                result,
                note,
                userAction,
                contentAction
            };
            
            console.log('处理举报:', request);
            const response = await api.post<ApiResponse<ReportResponse>>(`/admin/reports/${reportId}/handle`, request);
            console.log('处理举报成功:', response);
            return response;
        } catch (error) {
            console.error('处理举报失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取举报统计信息（仅管理员使用）
     *
     * @returns 统计信息
     */
    async getReportStatistics(): Promise<ApiResponse<any>> {
        try {
            console.log('获取举报统计信息');
            const response = await api.get<ApiResponse<any>>('/admin/reports/statistics');
            console.log('获取举报统计信息成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报统计信息失败:', error);
            throw error;
        }
    },
    
    /**
     * 获取举报内容详情
     *
     * @param contentType 内容类型(USER, MESSAGE, GROUP, GROUP_MEMBER, MOMENT等)
     * @param contentId 内容ID
     * @returns 内容详情
     */
    async getReportedContentDetails(contentType: string, contentId: number): Promise<ApiResponse<ReportContentDetails>> {
        try {
            console.log(`获取被举报内容详情: ${contentType} #${contentId}`);
            const response = await api.get<ApiResponse<ReportContentDetails>>(`/admin/reports/content/${contentType}/${contentId}`);
            console.log('获取举报内容详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报内容详情失败:', error);
            
            // 构造一个通用的错误响应
            return {
                success: false,
                message: error instanceof Error ? error.message : '获取举报内容详情失败',
                data: {} as ReportContentDetails
            };
        }
    },
    
    /**
     * 获取消息详情
     * 
     * @param messageId 消息ID
     * @returns 消息详情
     */
    async getMessageDetails(messageId: number): Promise<ApiResponse<any>> {
        try {
            console.log(`获取消息详情: #${messageId}`);
            const response = await api.get<ApiResponse<any>>(`/messages/${messageId}`);
            console.log('获取消息详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取消息详情失败:', error);
            return {
                success: false,
                message: error instanceof Error ? error.message : '获取消息详情失败',
                data: null
            };
        }
    },
    
    /**
     * 获取用户详情
     * 
     * @param userId 用户ID
     * @returns 用户详情
     */
    async getUserDetails(userId: number): Promise<ApiResponse<any>> {
        try {
            console.log(`获取用户详情: #${userId}`);
            const response = await api.get<ApiResponse<any>>(`/admin/users/${userId}`);
            console.log('获取用户详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取用户详情失败:', error);
            return {
                success: false,
                message: error instanceof Error ? error.message : '获取用户详情失败',
                data: null
            };
        }
    },
    
    /**
     * 获取群组详情
     * 
     * @param groupId 群组ID
     * @returns 群组详情
     */
    async getGroupDetails(groupId: number): Promise<ApiResponse<any>> {
        try {
            console.log(`获取群组详情: #${groupId}`);
            const response = await api.get<ApiResponse<any>>(`/admin/groups/${groupId}`);
            console.log('获取群组详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取群组详情失败:', error);
            return {
                success: false,
                message: error instanceof Error ? error.message : '获取群组详情失败',
                data: null
            };
        }
    },
    
    /**
     * 获取动态详情
     * 
     * @param momentId 动态ID
     * @returns 动态详情
     */
    async getMomentDetails(momentId: number): Promise<ApiResponse<any>> {
        try {
            console.log(`获取动态详情: #${momentId}`);
            const response = await api.get<ApiResponse<any>>(`/moments/${momentId}`);
            console.log('获取动态详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取动态详情失败:', error);
            return {
                success: false,
                message: error instanceof Error ? error.message : '获取动态详情失败',
                data: null
            };
        }
    }
};

/**
 * 分页响应接口
 */
interface Page<T> {
    /**
     * 当前页的内容
     */
    content: T[];
    
    /**
     * 是否为第一页
     */
    first: boolean;
    
    /**
     * 是否为最后一页
     */
    last: boolean;
    
    /**
     * 当前页码
     */
    number: number;
    
    /**
     * 每页大小
     */
    size: number;
    
    /**
     * 总页数
     */
    totalPages: number;
    
    /**
     * 总元素数
     */
    totalElements: number;
}

export default reportApi; 