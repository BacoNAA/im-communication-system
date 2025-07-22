import { api, type ApiResponse } from './request';

// 管理员认证
const getAdminToken = (): string | null => {
    // 尝试从多个可能的位置获取管理员令牌
    const locations = [
      { source: 'localStorage', key: 'accessToken' }, 
      { source: 'sessionStorage', key: 'accessToken' },
      { source: 'localStorage', key: 'adminToken' },
      { source: 'sessionStorage', key: 'adminToken' }
    ];
    
    for (const location of locations) {
      const token = location.source === 'localStorage' 
        ? localStorage.getItem(location.key) 
        : sessionStorage.getItem(location.key);
      
      if (token) {
        console.log(`获取到管理员令牌: 来源=${location.source}, 键=${location.key}`);
        return token;
      }
    }
    
    console.warn('未找到管理员令牌');
    return null;
};

export default {
    // 登录
    async login(username: string, password: string): Promise<ApiResponse<any>> {
        return api.post('/admin/auth/login', { username, password });
  },

    // 退出登录
    async logout(): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            return Promise.resolve({ code: 200, success: true, message: '退出成功', data: undefined });
        }

        const headers = { 'Authorization': `Bearer ${token}` };
        return api.post('/admin/auth/logout', {}, { headers });
  },

    // 重置密码
    async resetPassword(resetData: { username: string; newPassword: string }): Promise<ApiResponse<void>> {
        return api.post('/admin/auth/reset-password', resetData);
  },

    // 获取用户列表（分页）
    async getUserList(params: {
        page?: number; 
        size?: number; 
        keyword?: string; 
        status?: string;
    } = {}): Promise<ApiResponse<any>> {
        const queryParams = new URLSearchParams();
        
        if (params.page !== undefined) {
            queryParams.append('page', params.page.toString());
        }
        
        if (params.size !== undefined) {
            queryParams.append('size', params.size.toString());
        }
        
        if (params.keyword) {
            queryParams.append('keyword', params.keyword);
        }
        
        if (params.status) {
            queryParams.append('status', params.status);
      }
        
        const queryString = queryParams.toString();
        const url = `/admin/users${queryString ? `?${queryString}` : ''}`;
        
        return api.get(url);
    },
    
    // 获取用户详情
    async getUserDetails(userId: number): Promise<ApiResponse<any>> {
        return api.get(`/admin/users/${userId}`);
    },
    
    // 管理用户（封禁/解封）
    async manageUser(userId: number, action: string, reason: string, duration?: number): Promise<ApiResponse<any>> {
        const data = {
            userId,
            action,
            reason,
            duration
        };
        
        return api.post(`/admin/users/${userId}/manage`, data);
    },
    
    // 删除用户
    async deleteUser(userId: number): Promise<ApiResponse<any>> {
        return api.delete(`/admin/users/${userId}`);
    },

    // 获取群组列表（分页）
    async getGroupList(params: {
        page?: number;
        size?: number;
        keyword?: string;
        status?: string;
    } = {}): Promise<ApiResponse<any>> {
        const queryParams = new URLSearchParams();
        
        if (params.page !== undefined) {
            queryParams.append('page', params.page.toString());
        }
        
        if (params.size !== undefined) {
            queryParams.append('size', params.size.toString());
        }
        
        if (params.keyword) {
            queryParams.append('keyword', params.keyword);
        }
        
        if (params.status) {
            queryParams.append('status', params.status);
        }
        
        const queryString = queryParams.toString();
        const url = `/admin/groups${queryString ? `?${queryString}` : ''}`;
        
        return api.get(url);
    },
    
    // 获取群组详情
    async getGroupDetails(groupId: number): Promise<ApiResponse<any>> {
        return api.get(`/admin/groups/${groupId}`);
    },
    
    // 管理群组（封禁/解封/解散）
    async manageGroup(groupId: number, action: string, reason: string, duration?: number): Promise<ApiResponse<any>> {
        const params = new URLSearchParams();
        params.append('action', action);
        params.append('reason', reason);
        
        if (duration) {
            params.append('duration', duration.toString());
        }
        
        return api.post(`/admin/groups/${groupId}/manage?${params.toString()}`);
    },
    
    // 获取群组成员列表
    async getGroupMembers(groupId: number, page: number = 0, size: number = 20): Promise<ApiResponse<any>> {
        return api.get(`/admin/groups/${groupId}/members?page=${page}&size=${size}`);
  },
  
    // 获取系统通知列表（分页）
    async getSystemNotifications(params: {
        page?: number;
        size?: number;
        type?: string;
    } = {}): Promise<ApiResponse<any>> {
        const queryParams = new URLSearchParams();
        
        if (params.page !== undefined) {
            queryParams.append('page', params.page.toString());
        }
        
        if (params.size !== undefined) {
            queryParams.append('size', params.size.toString());
        }
        
        if (params.type) {
            queryParams.append('type', params.type);
        }
        
        const queryString = queryParams.toString();
        const url = `/admin/notifications${queryString ? `?${queryString}` : ''}`;
        
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.get(url, { headers });
    },
    
    // 获取系统通知详情
    async getSystemNotificationDetail(notificationId: number): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.get(`/admin/notifications/${notificationId}`, { headers });
    },
    
    // 创建系统通知
    async createSystemNotification(data: {
        title: string;
        content: string;
        type: string;
        targetUserIds?: number[];
    }): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        // 获取管理员ID
        const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
        let adminId = null;
        
        if (adminInfo) {
            try {
                const parsedInfo = JSON.parse(adminInfo);
                adminId = parsedInfo.id;
            } catch (e) {
                console.error('解析管理员信息失败:', e);
            }
        }
        
        if (!adminId) {
            // 从用户ID获取
            adminId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
        }
        
        if (!adminId) {
            throw new Error('无法获取管理员ID');
        }
        
        // 符合SystemNotificationRequest的字段
        const requestBody = {
            title: data.title,
            content: data.content,
            type: data.type,
            targetUserIds: data.targetUserIds || []
        };
        
        // 通过URL参数传递adminId
        const url = `/admin/notifications?adminId=${adminId}`;
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.post(url, requestBody, { headers });
    },
    
    // 更新系统通知
    async updateSystemNotification(notificationId: number, data: {
        title: string;
        content: string;
        type: string;
        targetUserIds?: number[];
    }): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        // 获取管理员ID
        const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
        let adminId = null;
        
        if (adminInfo) {
            try {
                const parsedInfo = JSON.parse(adminInfo);
                adminId = parsedInfo.id;
            } catch (e) {
                console.error('解析管理员信息失败:', e);
            }
        }
        
        if (!adminId) {
            // 从用户ID获取
            adminId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
        }
        
        if (!adminId) {
            throw new Error('无法获取管理员ID');
        }
        
        // 符合SystemNotificationRequest的字段
        const requestBody = {
            title: data.title,
            content: data.content,
            type: data.type,
            targetUserIds: data.targetUserIds || []
        };
        
        // 通过URL参数传递adminId
        const url = `/admin/notifications/${notificationId}?adminId=${adminId}`;
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.put(url, requestBody, { headers });
    },
    
    // 删除系统通知
    async deleteSystemNotification(notificationId: number): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        // 获取管理员ID
        const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
        let adminId = null;
        
        if (adminInfo) {
            try {
                const parsedInfo = JSON.parse(adminInfo);
                adminId = parsedInfo.id;
            } catch (e) {
                console.error('解析管理员信息失败:', e);
            }
        }
        
        if (!adminId) {
            // 从用户ID获取
            adminId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
        }
        
        if (!adminId) {
            throw new Error('无法获取管理员ID');
        }
        
        // 通过URL参数传递adminId
        const url = `/admin/notifications/${notificationId}?adminId=${adminId}`;
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.delete(url, { headers });
    },
    
    // 发布系统通知
    async publishSystemNotification(notificationId: number): Promise<ApiResponse<any>> {
        const token = getAdminToken();
        if (!token) {
            throw new Error('管理员未登录');
        }
        
        // 获取管理员ID
        const adminInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
        let adminId = null;
        
        if (adminInfo) {
            try {
                const parsedInfo = JSON.parse(adminInfo);
                adminId = parsedInfo.id;
            } catch (e) {
                console.error('解析管理员信息失败:', e);
            }
        }
        
        if (!adminId) {
            // 从用户ID获取
            adminId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
        }
        
        if (!adminId) {
            throw new Error('无法获取管理员ID');
        }
        
        // 确保adminId是Long类型（在Java中通过添加L后缀），将其转换为字符串
        const adminIdStr = adminId.toString();
        
        // 通过请求体传递adminId而不是URL参数
        const url = `/admin/notifications/${notificationId}/publish`;
        
        const headers = { 'Authorization': `Bearer ${token}` };
        return api.post(url, { adminId: adminIdStr }, { headers });
    }
}; 