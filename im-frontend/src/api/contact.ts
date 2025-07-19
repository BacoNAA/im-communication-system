import { api } from './request';
import type { ApiResponse } from './request';

// 联系人状态
export type ContactStatus = 'PENDING' | 'ACCEPTED' | 'BLOCKED' | 'DELETED';

// 用户信息接口
export interface ContactUser {
  id: number;
  email: string;
  nickname?: string;
  avatarUrl?: string;
  personalId?: string;
  status?: string;
  signature?: string;
}

// 联系人接口
export interface Contact {
  id: number;
  userId: number;
  friendId: number;
  alias?: string;
  status: ContactStatus;
  createdAt: string;
  updatedAt: string;
  friend: ContactUser;
  tags?: ContactTag[];
}

// 联系人标签接口
export interface ContactTag {
  id: number;
  name: string;
  color?: string;
}

// 好友请求接口
export interface FriendRequest {
  requestId: number;
  requesterId: number;
  requesterUsername: string;
  requesterNickname?: string;
  requesterAvatarUrl?: string;
  requesterUserIdStr?: string;
  recipientId: number;
  recipientUsername: string;
  recipientNickname?: string;
  recipientAvatarUrl?: string;
  recipientUserIdStr?: string;
  verificationMessage?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  statusDescription?: string;
  source?: string;
  createdAt: string;
  processedAt?: string;
  canProcess?: boolean;
  canWithdraw?: boolean;
}

// 添加好友请求接口
export interface AddFriendRequest {
  friendIdentifier: string; // 邮箱或个人ID
  message?: string;
}

// 搜索用户请求接口
export interface SearchUserRequest {
  keyword: string; // 邮箱或个人ID
}

// 设置联系人别名请求接口
export interface SetContactAliasRequest {
  alias: string;
}

// 分配标签请求接口
export interface AssignTagRequest {
  tagIds: number[];
}

export const contactApi = {
  // 获取联系人列表
  async getContacts(userId: number, includeBlocked: boolean = false): Promise<ApiResponse<Contact[]>> {
    console.log('contactApi.getContacts: 开始请求, 用户ID:', userId, '包含已屏蔽:', includeBlocked);
    
    if (!userId || userId <= 0) {
      console.error('contactApi.getContacts: 无效的用户ID:', userId);
      return {
        code: 400,
        message: '无效的用户ID',
        data: [],
        success: false
      };
    }
    
    // 获取认证令牌
    const token = localStorage.getItem('accessToken') || 
                 localStorage.getItem('auth_token') || 
                 sessionStorage.getItem('accessToken') || 
                 sessionStorage.getItem('auth_token');
                 
    console.log('contactApi.getContacts: 认证令牌存在:', !!token);
    
    if (!token) {
      console.error('contactApi.getContacts: 未找到认证令牌');
      return {
        code: 401,
        message: '未授权，请先登录',
        data: [],
        success: false
      };
    }
    
    const headers: Record<string, string> = {
      'X-User-Id': userId.toString(),
      'Authorization': `Bearer ${token}`
    };
    
    // 尝试不同的API路径
    const apiPaths = [
      `/contacts?userId=${userId}&includeBlocked=${includeBlocked}`,
      `/contact/list?userId=${userId}&includeBlocked=${includeBlocked}`,
      `/user/${userId}/contacts?includeBlocked=${includeBlocked}`
    ];
    
    let lastError: any = null;
    let lastResponse: any = null;
    
    // 依次尝试不同的API路径
    for (const path of apiPaths) {
      try {
        console.log('contactApi.getContacts: 尝试请求URL:', path);
        console.log('contactApi.getContacts: 请求头:', headers);
        
        const response = await api.get<ApiResponse<Contact[]>>(path, { headers });
        
        console.log('contactApi.getContacts: 响应结果:', response);
        lastResponse = response;
        
        // 检查响应是否包含有效数据
        if (response.success && Array.isArray(response.data)) {
          // 检查数据结构，确保每个联系人都有friend字段
          const validData = response.data.filter((contact: any) => {
            if (!contact.friend) {
              console.warn('contactApi.getContacts: 联系人缺少friend字段:', contact);
              
              // 尝试从其他字段构建friend字段
              if (contact.friendId && (contact.friendEmail || contact.email || contact.friendName || contact.nickname)) {
                contact.friend = {
                  id: contact.friendId,
                  email: contact.friendEmail || contact.email || '',
                  nickname: contact.friendName || contact.nickname || '',
                  avatarUrl: contact.friendAvatarUrl || contact.avatarUrl || '',
                };
                return true;
              }
              return false;
            }
            return true;
          });
          
          console.log(`contactApi.getContacts: 路径 ${path} 成功返回 ${validData.length} 条有效联系人数据`);
          
          // 返回有效的数据
          return {
            ...response,
            data: validData
          };
        } else {
          console.warn(`contactApi.getContacts: 路径 ${path} 响应无效数据:`, response);
          // 继续尝试下一个路径
        }
      } catch (error) {
        console.error(`contactApi.getContacts: 路径 ${path} 请求失败:`, error);
        lastError = error;
        // 继续尝试下一个路径
      }
    }
    
    // 如果有最后一个响应但不包含有效数据，返回该响应
    if (lastResponse) {
      console.warn('contactApi.getContacts: 所有路径都未返回有效数据，返回最后一个响应');
      return lastResponse;
    }
    
    // 所有路径都失败了，抛出最后一个错误
    console.error('contactApi.getContacts: 所有API路径都请求失败');
    throw lastError || new Error('所有API路径都请求失败');
  },

  // 测试联系人API
  async testContactsAPI(userId: number): Promise<void> {
    console.log('开始测试联系人API');
    
    // 获取认证令牌
    const token = localStorage.getItem('accessToken') || 
                 localStorage.getItem('auth_token') || 
                 sessionStorage.getItem('accessToken') || 
                 sessionStorage.getItem('auth_token');
    
    if (!token) {
      console.error('测试失败: 未找到认证令牌');
      return;
    }
    
    const headers = {
      'Authorization': `Bearer ${token}`,
      'X-User-Id': userId.toString(),
      'Content-Type': 'application/json'
    };
    
    try {
      // 直接使用fetch API进行测试
      const response = await fetch(`/api/contacts?userId=${userId}`, {
        method: 'GET',
        headers
      });
      
      console.log('测试响应状态:', response.status, response.statusText);
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('测试失败:', errorText);
        return;
      }
      
      const data = await response.json();
      console.log('测试成功，响应数据:', data);
    } catch (error) {
      console.error('测试过程中出错:', error);
    }
  },

  // 搜索用户
  async searchUser(searchData: SearchUserRequest, userId: number): Promise<ApiResponse<ContactUser[]>> {
    return api.post<ApiResponse<ContactUser[]>>('/users/search', searchData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 发送好友请求
  async sendFriendRequest(requestData: AddFriendRequest, userId: number): Promise<ApiResponse<FriendRequest>> {
    return api.post<ApiResponse<FriendRequest>>('/contact-requests/send', requestData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 获取好友请求列表
  async getFriendRequests(userId: number, type: 'sent' | 'received' = 'received'): Promise<ApiResponse<FriendRequest[]>> {
    if (type === 'received') {
      return api.get<ApiResponse<FriendRequest[]>>('/contact-requests/received');
    } else {
      return api.get<ApiResponse<FriendRequest[]>>('/contact-requests/sent');
    }
  },

  // 处理好友请求（接受/拒绝）
  async handleFriendRequest(requestId: number, action: 'accept' | 'reject', userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contact-requests/${requestId}/${action}`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 删除好友
  async deleteFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.delete<ApiResponse<string>>(`/contacts/${friendId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 拉黑好友
  async blockFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contacts/${friendId}/block`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 取消拉黑
  async unblockFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contacts/${friendId}/unblock`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 设置联系人别名
  async setContactAlias(friendId: number, aliasData: SetContactAliasRequest, userId: number): Promise<ApiResponse<string>> {
    // 确保friendId是有效的数字
    if (isNaN(friendId) || friendId <= 0) {
      console.error('无效的联系人ID:', friendId);
      return {
        code: 400,
        message: '无效的联系人ID',
        data: '',
        success: false
      };
    }
    
    console.log('设置联系人别名:', {
      friendId,
      alias: aliasData.alias,
      userId
    });
    
    return api.put<ApiResponse<string>>(`/contacts/${friendId}/alias`, aliasData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 获取联系人标签
  async getContactTags(userId: number): Promise<ApiResponse<ContactTag[]>> {
    return api.get<ApiResponse<ContactTag[]>>(`/contact-tags?userId=${userId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 创建联系人标签
  async createContactTag(tagData: { name: string; color?: string }, userId: number): Promise<ApiResponse<ContactTag>> {
    return api.post<ApiResponse<ContactTag>>('/contact-tags', tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 更新联系人标签
  async updateContactTag(tagId: number, tagData: { name: string; color?: string }, userId: number): Promise<ApiResponse<ContactTag>> {
    return api.put<ApiResponse<ContactTag>>(`/contact-tags/${tagId}`, tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 删除联系人标签
  async deleteContactTag(tagId: number, userId: number): Promise<ApiResponse<string>> {
    return api.delete<ApiResponse<string>>(`/contact-tags/${tagId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 为联系人分配标签
  async assignContactTags(friendId: number, tagData: AssignTagRequest, userId: number): Promise<ApiResponse<string>> {
    // 确保friendId是有效的数字
    if (isNaN(friendId) || friendId <= 0) {
      console.error('无效的联系人ID:', friendId);
      return {
        code: 400,
        message: '无效的联系人ID',
        data: '',
        success: false
      };
    }
    
    console.log('为联系人分配标签:', {
      friendId,
      tagIds: tagData.tagIds,
      userId
    });
    
    return api.put<ApiResponse<string>>(`/contacts/${friendId}/tags`, tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  }
};