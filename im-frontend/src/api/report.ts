import { api, type ApiResponse } from './request';

// 管理员认证
const getAdminToken = (): string | null => {
    console.log('尝试获取管理员令牌...');
    
    // 尝试从多个可能的位置获取管理员令牌
    const locations = [
      { source: 'localStorage', key: 'adminToken' },
      { source: 'sessionStorage', key: 'adminToken' },
      { source: 'localStorage', key: 'accessToken' }, 
      { source: 'sessionStorage', key: 'accessToken' },
      { source: 'localStorage', key: 'token' },
      { source: 'sessionStorage', key: 'token' },
      { source: 'localStorage', key: 'auth_token' },
      { source: 'sessionStorage', key: 'auth_token' }
    ];
    
    // 检查所有可能的位置并记录日志
    console.log('所有可能的令牌位置状态:');
    locations.forEach(location => {
        const value = location.source === 'localStorage' 
            ? localStorage.getItem(location.key) 
            : sessionStorage.getItem(location.key);
            
        console.log(`- ${location.source}.${location.key}: ${value ? '存在' : '不存在'} ${value ? `(长度: ${value.length})` : ''}`);
    });
    
    for (const location of locations) {
      const token = location.source === 'localStorage' 
        ? localStorage.getItem(location.key) 
        : sessionStorage.getItem(location.key);
      
      if (token) {
        console.log(`获取到管理员令牌: 来源=${location.source}, 键=${location.key}, 前10位=${token.substring(0, 10)}...`);
        
        // 检查令牌是否为有效JWT格式
        if (token.split('.').length === 3) {
          console.log('令牌看起来是有效的JWT格式');
          return token;
        } else {
          console.warn('获取到的令牌不是有效的JWT格式，尝试下一个位置');
        }
      }
    }
    
    // 检查localStorage中的adminInfo
    const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
    if (adminInfo) {
      console.log('找到adminInfo, 尝试提取令牌');
      try {
        const parsedInfo = JSON.parse(adminInfo);
        if (parsedInfo.token) {
          console.log(`从adminInfo中提取到令牌, 前10位=${parsedInfo.token.substring(0, 10)}...`);
          return parsedInfo.token;
        }
      } catch (e) {
        console.error('解析adminInfo失败:', e);
      }
    }
    
    console.warn('未找到管理员令牌');
    return null;
};

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
     * 举报者头像URL
     */
    reporterAvatarUrl?: string;
    
    /**
     * 被举报用户ID（如适用）
     */
    reportedUserId?: number;
    
    /**
     * 被举报用户名（如适用）
     */
    reportedUsername?: string;
    
    /**
     * 被举报用户头像URL（如适用）
     */
    reportedUserAvatarUrl?: string;
    
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
                reportedContentId: groupId, // 修改为使用群组ID，而不是成员ID
                reportedUserId: memberId,   // 被举报用户ID保持不变
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
    async getReportList(page: number = 0, size: number = 10, status?: string, contentType?: string, searchParams?: any): Promise<ApiResponse<Page<ReportResponse>>> {
        try {
            // 获取管理员令牌
            const token = getAdminToken();
            if (!token) {
                console.error('获取举报列表失败: 未找到管理员令牌');
                throw new Error('未登录或登录已过期');
            }
            
            // 构建请求头
            const headers = { 'Authorization': `Bearer ${token}` };
            
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
            
            // 添加搜索参数
            if (searchParams) {
                if (searchParams.userId) {
                    params.append('userId', searchParams.userId);
                }
                if (searchParams.groupId) {
                    params.append('groupId', searchParams.groupId);
                }
                if (searchParams.reason) {
                    params.append('reason', searchParams.reason);
                }
            }
            
            console.log('获取举报列表:', { page, size, status, contentType, searchParams });
            const response = await api.get<ApiResponse<Page<ReportResponse>>>(`/admin/reports?${params.toString()}`, { headers });
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
            // 获取管理员令牌
            const token = getAdminToken();
            if (!token) {
                console.error('获取举报详情失败: 未找到管理员令牌');
                throw new Error('未登录或登录已过期');
            }
            
            // 构建请求头
            const headers = { 'Authorization': `Bearer ${token}` };
            
            console.log('获取举报详情:', reportId);
            const response = await api.get<ApiResponse<ReportResponse>>(`/admin/reports/${reportId}`, { headers });
            console.log('获取举报详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报详情失败:', error);
            throw error;
        }
    },

    /**
     * 获取被举报内容详情（仅管理员使用）
     *
     * @param contentType 内容类型 (USER, MESSAGE, GROUP, GROUP_MEMBER, MOMENT等)
     * @param contentId 内容ID
     * @returns 被举报内容的详细信息
     */
    async getReportedContentDetails(contentType: string, contentId: number): Promise<ApiResponse<any>> {
        try {
            // 获取管理员令牌
            const token = getAdminToken();
            if (!token) {
                console.error('获取被举报内容详情失败: 未找到管理员令牌');
                throw new Error('未登录或登录已过期');
            }
            
            // 构建请求头
            const headers = { 'Authorization': `Bearer ${token}` };
            
            console.log('获取被举报内容详情:', { contentType, contentId });
            const response = await api.get<ApiResponse<any>>(`/admin/reports/content/${contentType}/${contentId}`, { headers });
            console.log('获取被举报内容详情成功:', response);
            return response;
        } catch (error) {
            console.error('获取被举报内容详情失败:', error);
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
     * @param banDuration 封禁时长(小时)，仅在临时封禁操作时使用
     * @returns 处理结果
     */
    async handleReport(reportId: number, action: string, result: string, note?: string, userAction?: string, contentAction?: string, banDuration?: number): Promise<ApiResponse<ReportResponse>> {
        try {
            // 获取管理员令牌
            const token = getAdminToken();
            if (!token) {
                console.error('处理举报失败: 未找到管理员令牌');
                throw new Error('未登录或登录已过期');
            }
            
            // 获取管理员ID
            const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
            let adminId: number | null = null;
            
            if (adminInfo) {
                try {
                    const parsedInfo = JSON.parse(adminInfo);
                    if (typeof parsedInfo.id === 'number') {
                        adminId = parsedInfo.id;
                    } else if (typeof parsedInfo.id === 'string') {
                        adminId = parseInt(parsedInfo.id, 10);
                        if (isNaN(adminId)) {
                            adminId = null;
                        }
                    }
                } catch (e) {
                    console.error('解析管理员信息失败:', e);
                }
            }
            
            if (!adminId) {
                // 尝试从其他地方获取
                const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
                if (userId) {
                    adminId = parseInt(userId, 10);
                }
            }
            
            if (!adminId) {
                console.error('处理举报失败: 未找到管理员ID');
                throw new Error('未能获取管理员ID');
            }
            
            // 构建请求头和请求体
            const headers = { 'Authorization': `Bearer ${token}` };
            
            const request = {
                // 移除reportId，因为已经在URL路径中包含
                action,
                result,
                note,
                userAction,
                contentAction,
                banDuration
                // adminId已移除，让后端从认证上下文中获取
            };
            
            console.log('处理举报:', request);
            
            // 直接使用正确的URL路径，不添加adminId查询参数
            const url = `/admin/reports/${reportId}/handle`;
            
            // 发送请求
            const response = await api.post<ApiResponse<ReportResponse>>(url, request, { headers });
            
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
            // 获取管理员令牌
            const token = getAdminToken();
            if (!token) {
                console.error('获取举报统计信息失败: 未找到管理员令牌');
                throw new Error('未登录或登录已过期');
            }
            
            // 构建请求头
            const headers = { 'Authorization': `Bearer ${token}` };
            
            console.log('获取举报统计信息');
            const response = await api.get<ApiResponse<any>>('/admin/reports/statistics', { headers });
            console.log('获取举报统计信息成功:', response);
            return response;
        } catch (error) {
            console.error('获取举报统计信息失败:', error);
            throw error;
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